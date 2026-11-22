package com.monef.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.monef.dto.RateView;
import com.monef.exception.ApiException;
import com.monef.security.RateLimiter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Anonymous indicative quotes. Never used as the ledger's accounting rate. */
@Service
public class DelayedFxService {

    private final RestClient client;
    private final Clock clock;
    private final RateLimiter limiter;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Quote(
        String result,
        String base,
        String quote,
        BigDecimal rate,
        String source,
        @JsonProperty("data_updated_at") Instant dataUpdatedAt,
        @JsonProperty("market_session") String marketSession,
        boolean stale
    ) {}

    public DelayedFxService(
        RestClient.Builder builder,
        @Value("${monef.fx-delayed-url:https://api.exchangerate.dev}") String url,
        Clock clock,
        RateLimiter limiter
    ) {
        this.client = builder
            .clone()
            .baseUrl(url)
            .defaultHeader("User-Agent", "Monef-Finance/1.0")
            .build();
        this.clock = clock;
        this.limiter = limiter;
    }

    public RateView rate(String base, String quote) {
        if (base.equals(quote)) return new RateView(
            base,
            quote,
            BigDecimal.ONE,
            clock.instant().atZone(ZoneOffset.UTC).toLocalDate(),
            "IDENTITY",
            false,
            clock.instant()
        );
        limiter.check("fx:provider:exchangerate-dev:minute", 7, 60);
        limiter.check("fx:provider:exchangerate-dev:hour", 90, 3600);
        try {
            var response = client
                .get()
                .uri("/v1/rate/{pair}", (base + "-" + quote).toLowerCase(Locale.ROOT))
                .retrieve()
                .toEntity(Quote.class);
            Quote result = response.getBody();
            if (
                result == null ||
                !"success".equals(result.result()) ||
                !base.equals(result.base()) ||
                !quote.equals(result.quote()) ||
                result.rate() == null ||
                result.rate().signum() <= 0 ||
                result.dataUpdatedAt() == null ||
                result.dataUpdatedAt().isAfter(clock.instant().plusSeconds(60)) ||
                result.source() == null ||
                !Set.of("live", "ecb_daily", "market_daily").contains(result.source())
            ) {
                throw new ApiException(503, "延迟行情服务返回了无效数据，请稍后重试");
            }
            String delayHeader = response.getHeaders().getFirst("X-Price-Delay-Seconds");
            int delay = "live".equals(result.source())
                ? delayHeader == null
                    ? 900
                    : Integer.parseInt(delayHeader)
                : 0;
            if (delay < 0) throw new ApiException(503, "行情延迟信息异常，请稍后重试");
            return new RateView(
                base,
                quote,
                result.rate().setScale(10, RoundingMode.HALF_UP),
                result.dataUpdatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                "EXCHANGERATE_DEV",
                false,
                clock.instant(),
                result.dataUpdatedAt(),
                delay,
                result.source(),
                result.marketSession(),
                result.stale()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) throw new ApiException(
                429,
                "免费行情调用次数已达上限，请稍后重试"
            );
            throw new ApiException(503, "延迟行情暂时不可用，本次未使用旧报价计算");
        } catch (Exception exception) {
            throw new ApiException(503, "延迟行情暂时不可用，本次未使用旧报价计算");
        }
    }
}
