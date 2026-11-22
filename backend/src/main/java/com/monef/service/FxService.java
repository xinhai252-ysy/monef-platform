package com.monef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monef.dto.Conversion;
import com.monef.dto.PageView;
import com.monef.dto.RateView;
import com.monef.entity.FxRate;
import com.monef.exception.ApiException;
import com.monef.repository.FxRepository;
import java.math.*;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FxService {

    public static final Set<String> CURRENCIES = Set.of(
        "USD",
        "EUR",
        "GBP",
        "CNY",
        "JPY",
        "SGD",
        "HKD",
        "AUD",
        "CAD",
        "CHF"
    );
    private final FxRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Clock clock;
    private final boolean demo, redisEnabled;
    private final RestClient client;
    private final LiveFxService liveFx;
    private final DelayedFxService delayedFx;
    private final String liveProvider;

    private record Cached(RateView rate, Instant expires) {}

    private record RemoteRate(String base, String quote, BigDecimal rate, LocalDate date) {}

    private final Map<String, Cached> memory = new ConcurrentHashMap<>();

    public FxService(
        FxRepository repo,
        StringRedisTemplate redis,
        ObjectMapper json,
        Clock clock,
        @Value("${monef.fx-demo}") boolean demo,
        @Value("${monef.redis-enabled}") boolean redisEnabled,
        @Value("${monef.fx-url}") String url,
        @Value("${monef.fx-live-provider:frankfurter}") String liveProvider,
        LiveFxService liveFx,
        DelayedFxService delayedFx
    ) {
        this.repo = repo;
        this.redis = redis;
        this.json = json;
        this.clock = clock;
        this.demo = demo;
        this.liveProvider = liveProvider;
        this.liveFx = liveFx;
        this.delayedFx = delayedFx;
        if (!Set.of("frankfurter", "twelvedata", "exchangerate_dev").contains(liveProvider)) {
            throw new IllegalArgumentException("Unsupported FX_LIVE_PROVIDER");
        }
        this.redisEnabled = redisEnabled;
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public String currency(String value) {
        if (
            value == null || !CURRENCIES.contains(value.toUpperCase(Locale.ROOT))
        ) throw new ApiException(400, "暂不支持该币种");
        return value.toUpperCase(Locale.ROOT);
    }

    public RateView rate(String from, String to, LocalDate requested) {
        String base = currency(from),
            quote = currency(to);
        LocalDate today = LocalDate.now(clock);
        if (
            requested != null &&
            (requested.isAfter(today) || requested.isBefore(LocalDate.of(2000, 1, 1)))
        ) throw new ApiException(400, "汇率日期需介于 2000-01-01 与今天之间");
        if (base.equals(quote)) return new RateView(
            base,
            quote,
            BigDecimal.ONE,
            requested == null ? today : requested,
            "IDENTITY",
            false,
            clock.instant()
        );
        String source = demo ? "DEMO" : "FRANKFURTER";
        String key =
            "monef:fx:" +
            source +
            ":" +
            base +
            ":" +
            quote +
            ":" +
            (requested == null ? "latest" : requested);
        RateView cached = readCache(key);
        if (cached != null) return new RateView(
            cached.base(),
            cached.quote(),
            cached.rate(),
            cached.date(),
            cached.source(),
            true,
            cached.fetchedAt()
        );
        if (requested != null) {
            var saved = repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateAndSource(
                base,
                quote,
                requested,
                source
            );
            if (saved.isPresent()) {
                var view = view(saved.get(), true);
                writeCache(key, view);
                return view;
            }
        }
        BigDecimal value;
        LocalDate date;
        if (demo) {
            Map<String, BigDecimal> rates = new HashMap<>();
            String[] codes = {
                "USD",
                "EUR",
                "GBP",
                "CNY",
                "JPY",
                "SGD",
                "HKD",
                "AUD",
                "CAD",
                "CHF",
            };
            String[] values = {
                "1",
                "0.92",
                "0.77",
                "7.12",
                "150",
                "1.30",
                "7.78",
                "1.49",
                "1.36",
                "0.87",
            };
            for (int i = 0; i < codes.length; i++) rates.put(codes[i], new BigDecimal(values[i]));
            value = rates.get(quote).divide(rates.get(base), 10, RoundingMode.HALF_UP);
            date = requested == null ? today : requested;
        } else {
            try {
                RemoteRate remote = client
                    .get()
                    .uri(u -> {
                        var b = u.path("/v2/rate/{base}/{quote}");
                        if (requested != null) b.queryParam("date", requested);
                        return b.build(
                            base.toLowerCase(Locale.ROOT),
                            quote.toLowerCase(Locale.ROOT)
                        );
                    })
                    .retrieve()
                    .body(RemoteRate.class);
                if (
                    remote == null ||
                    remote.rate() == null ||
                    remote.rate().signum() <= 0 ||
                    remote.date() == null ||
                    !base.equalsIgnoreCase(remote.base()) ||
                    !quote.equalsIgnoreCase(remote.quote()) ||
                    remote.date().isAfter(requested == null ? today : requested)
                ) throw new IllegalStateException("Invalid rate observation");
                value = remote.rate().setScale(10, RoundingMode.HALF_UP);
                date = remote.date();
            } catch (Exception e) {
                throw new ApiException(
                    503,
                    "汇率供应商暂时不可用，未使用过期或虚构汇率，请稍后重试"
                );
            }
        }
        var entity = repo
            .findByBaseCurrencyAndQuoteCurrencyAndRateDateAndSource(base, quote, date, source)
            .orElse(new FxRate());
        entity.setBaseCurrency(base);
        entity.setQuoteCurrency(quote);
        entity.setRateDate(date);
        entity.setRate(value);
        entity.setSource(source);
        entity.setFetchedAt(clock.instant());
        try {
            repo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            entity = repo
                .findByBaseCurrencyAndQuoteCurrencyAndRateDateAndSource(base, quote, date, source)
                .orElseThrow(() -> e);
        }
        var result = view(entity, false);
        writeCache(key, result);
        return result;
    }

    public Conversion convert(String base, String quote, BigDecimal amount, LocalDate date) {
        if (
            amount == null ||
            amount.signum() < 0 ||
            amount.compareTo(new BigDecimal("9999999999999.99")) > 0 ||
            amount.scale() > 2
        ) throw new ApiException(400, "请输入有效金额（最多两位小数）");
        RateView r;
        if (!demo && date == null && "exchangerate_dev".equals(liveProvider)) {
            r = delayedFx.rate(currency(base), currency(quote));
        } else if (!demo && date == null && "twelvedata".equals(liveProvider)) {
            r = liveFx.rate(currency(base), currency(quote));
        } else {
            r = rate(base, quote, date);
        }
        return new Conversion(
            r.base(),
            r.quote(),
            amount,
            amount.multiply(r.rate()).setScale(2, RoundingMode.HALF_UP),
            r
        );
    }

    public PageView<RateView> history(String base, String quote, int page) {
        if (page < 0) throw new ApiException(400, "页码不能为负数");
        var p = repo.findByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(
            currency(base),
            currency(quote),
            PageRequest.of(page, 20)
        );
        return new PageView<>(
            p.map(e -> view(e, true)).getContent(),
            page,
            20,
            p.getTotalElements(),
            p.getTotalPages()
        );
    }

    private RateView view(FxRate e, boolean cached) {
        return new RateView(
            e.getBaseCurrency(),
            e.getQuoteCurrency(),
            e.getRate(),
            e.getRateDate(),
            e.getSource(),
            cached,
            e.getFetchedAt()
        );
    }

    private RateView readCache(String key) {
        if (redisEnabled) {
            try {
                String v = redis.opsForValue().get(key);
                return v == null ? null : json.readValue(v, RateView.class);
            } catch (Exception e) {
                return null;
            }
        }
        var v = memory.get(key);
        if (v != null && v.expires().isAfter(clock.instant())) return v.rate();
        memory.remove(key);
        return null;
    }

    private void writeCache(String key, RateView value) {
        if (redisEnabled) {
            try {
                redis.opsForValue().set(key, json.writeValueAsString(value), Duration.ofHours(1));
            } catch (Exception ignored) {}
        } else {
            if (memory.size() > 1000) memory.clear();
            memory.put(key, new Cached(value, clock.instant().plusSeconds(3600)));
        }
    }
}
