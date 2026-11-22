package com.monef.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.monef.dto.RateView;
import com.monef.exception.ApiException;
import com.monef.security.RateLimiter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Latest conversion quotes only. Ledger snapshots continue using daily reference rates. */
@Service
public class LiveFxService {

    private final RestClient client;
    private final String apiKey;
    private final Clock clock;
    private final RateLimiter limiter;
    private final int requestsPerMinute;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Quote(String symbol, BigDecimal rate, Long timestamp, String status, Integer code) {}

    public LiveFxService(
        RestClient.Builder builder,
        @Value("${monef.fx-live-api-key:}") String apiKey,
        @Value("${monef.fx-live-url:https://api.twelvedata.com}") String url,
        @Value("${monef.fx-live-requests-per-minute:7}") int requestsPerMinute,
        Clock clock,
        RateLimiter limiter
    ) {
        this.apiKey = apiKey;
        this.client = builder.clone().baseUrl(url).build();
        this.clock = clock;
        this.limiter = limiter;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
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
        if (apiKey.isBlank()) throw new ApiException(
            503,
            "分钟行情尚未启用，请在服务器配置 TWELVE_DATA_API_KEY"
        );
        // Provider-wide budget is shared across users and application instances through Redis.
        limiter.check("fx:provider:twelvedata", requestsPerMinute, 60);
        try {
            Quote result = client
                .get()
                .uri(uri ->
                    uri
                        .path("/exchange_rate")
                        .queryParam("symbol", base + "/" + quote)
                        .queryParam("timezone", "UTC")
                        .queryParam("dp", 10)
                        .build()
                )
                .header("Authorization", "apikey " + apiKey)
                .retrieve()
                .body(Quote.class);
            if (result != null && "error".equals(result.status())) {
                if (Integer.valueOf(429).equals(result.code())) throw new ApiException(
                    429,
                    "行情服务调用额度已用完，请稍后重试"
                );
                throw new ApiException(503, "行情服务拒绝请求，请检查密钥权限和币种支持");
            }
            if (
                result == null ||
                !(base + "/" + quote).equals(result.symbol()) ||
                result.rate() == null ||
                result.rate().signum() <= 0 ||
                result.timestamp() == null ||
                result.timestamp() <= 0
            ) {
                throw new ApiException(503, "行情服务返回了无效报价，请稍后重试");
            }
            Instant quotedAt = Instant.ofEpochSecond(result.timestamp());
            if (quotedAt.isAfter(clock.instant().plusSeconds(60))) {
                throw new ApiException(503, "行情报价时间异常，请稍后重试");
            }
            return new RateView(
                base,
                quote,
                result.rate().setScale(10, RoundingMode.HALF_UP),
                quotedAt.atZone(ZoneOffset.UTC).toLocalDate(),
                "TWELVE_DATA",
                false,
                clock.instant(),
                quotedAt
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) throw new ApiException(
                429,
                "行情服务调用额度已用完，请稍后重试"
            );
            throw new ApiException(503, "行情服务请求失败，请检查密钥、套餐权限或稍后重试");
        } catch (Exception exception) {
            // Do not log provider request details or propagate API keys / upstream bodies.
            throw new ApiException(503, "分钟行情暂时不可用，本次未使用旧汇率计算");
        }
    }
}
