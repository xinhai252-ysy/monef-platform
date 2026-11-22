package com.monef;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.monef.exception.ApiException;
import com.monef.security.RateLimiter;
import com.monef.service.DelayedFxService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DelayedFxServiceTest {

    private final Instant now = Instant.parse("2026-10-27T22:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final RateLimiter limiter = mock(RateLimiter.class);

    private String quote(String rate, String source) {
        return (
            "{\"result\":\"success\",\"base\":\"USD\",\"quote\":\"EUR\",\"rate\":" +
            rate +
            ",\"source\":\"" +
            source +
            "\",\"data_updated_at\":\"2026-10-27T21:44:59Z\"," +
            "\"timestamp\":\"2026-10-27T22:00:00Z\",\"market_session\":\"open\",\"stale\":false}"
        );
    }

    @Test
    void preservesDelayAndObservationTimeAndFetchesEveryTime() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new DelayedFxService(builder, "https://provider.test", clock, limiter);
        for (String rate : new String[] { "0.89", "0.90" }) {
            server
                .expect(requestTo("https://provider.test/v1/rate/usd-eur"))
                .andRespond(
                    withSuccess(quote(rate, "live"), MediaType.APPLICATION_JSON).header(
                        "X-Price-Delay-Seconds",
                        "900"
                    )
                );
        }
        var first = service.rate("USD", "EUR");
        var second = service.rate("USD", "EUR");
        assertThat(first.rate()).isEqualByComparingTo("0.89");
        assertThat(second.rate()).isEqualByComparingTo("0.90");
        assertThat(second.delaySeconds()).isEqualTo(900);
        assertThat(second.quotedAt()).isEqualTo(Instant.parse("2026-10-27T21:44:59Z"));
        assertThat(second.fetchedAt()).isEqualTo(now);
        assertThat(second.cached()).isFalse();
        assertThat(second.source()).isEqualTo("EXCHANGERATE_DEV");
        verify(limiter, times(2)).check("fx:provider:exchangerate-dev:minute", 7, 60);
        verify(limiter, times(2)).check("fx:provider:exchangerate-dev:hour", 90, 3600);
        server.verify();
    }

    @Test
    void keepsDailyFallbackMetadataInsteadOfCallingItIntraday() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new DelayedFxService(builder, "https://provider.test", clock, limiter);
        server
            .expect(anything())
            .andRespond(withSuccess(quote("0.89", "ecb_daily"), MediaType.APPLICATION_JSON));
        var result = service.rate("USD", "EUR");
        assertThat(result.providerSource()).isEqualTo("ecb_daily");
        assertThat(result.delaySeconds()).isZero();
        server.verify();
    }

    @Test
    void rejectsWrongPairAndProviderQuotaFailures() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new DelayedFxService(builder, "https://provider.test", clock, limiter);
        server
            .expect(anything())
            .andRespond(
                withSuccess(
                    quote("0.89", "live").replace("\"EUR\"", "\"JPY\""),
                    MediaType.APPLICATION_JSON
                )
            );
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> service.rate("USD", "EUR"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("无效");
        assertThatThrownBy(() -> service.rate("USD", "EUR"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("上限");
        server.verify();
    }
}
