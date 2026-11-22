package com.monef;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.monef.exception.ApiException;
import com.monef.security.RateLimiter;
import com.monef.service.LiveFxService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LiveFxServiceTest {

    private final Instant now = Instant.parse("2026-10-27T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final RateLimiter limiter = mock(RateLimiter.class);

    @Test
    void queriesEveryTimeKeepsProviderTimeAndSendsSecretInHeader() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new LiveFxService(
            builder,
            "test-secret",
            "https://provider.test",
            7,
            clock,
            limiter
        );
        for (String rate : new String[] { "0.9201", "0.9202" }) {
            server
                .expect(
                    requestTo(
                        "https://provider.test/exchange_rate?symbol=USD/EUR&timezone=UTC&dp=10"
                    )
                )
                .andExpect(header("Authorization", "apikey test-secret"))
                .andRespond(
                    withSuccess(
                        "{\"symbol\":\"USD/EUR\",\"rate\":" +
                            rate +
                            ",\"timestamp\":" +
                            now.minusSeconds(30).getEpochSecond() +
                            "}",
                        MediaType.APPLICATION_JSON
                    )
                );
        }
        var first = service.rate("USD", "EUR");
        var second = service.rate("USD", "EUR");
        assertThat(first.rate()).isEqualByComparingTo("0.9201");
        assertThat(second.rate()).isEqualByComparingTo("0.9202");
        assertThat(second.cached()).isFalse();
        assertThat(second.source()).isEqualTo("TWELVE_DATA");
        assertThat(second.quotedAt()).isEqualTo(now.minusSeconds(30));
        assertThat(second.fetchedAt()).isEqualTo(now);
        verify(limiter, times(2)).check("fx:provider:twelvedata", 7, 60);
        server.verify();
    }

    @Test
    void missingCredentialsFailExplicitlyWithoutCallingProvider() {
        var service = new LiveFxService(
            RestClient.builder(),
            "",
            "https://provider.test",
            7,
            clock,
            limiter
        );
        assertThatThrownBy(() -> service.rate("USD", "EUR"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("尚未启用");
        verifyNoInteractions(limiter);
        assertThat(service.rate("USD", "USD").rate()).isEqualByComparingTo("1");
    }

    @Test
    void rejectsWrongPairAndFutureTimestamp() {
        for (String body : new String[] {
            "{\"symbol\":\"EUR/USD\",\"rate\":1.1,\"timestamp\":" + now.getEpochSecond() + "}",
            "{\"symbol\":\"USD/EUR\",\"rate\":0.9,\"timestamp\":" +
                now.plusSeconds(600).getEpochSecond() +
                "}",
        }) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var service = new LiveFxService(
                builder,
                "test-secret",
                "https://provider.test",
                7,
                clock,
                limiter
            );
            server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThatThrownBy(() -> service.rate("USD", "EUR")).isInstanceOf(ApiException.class);
            server.verify();
        }
    }

    @Test
    void reportsQuotaAndProviderFailuresWithoutLeakingUpstreamBody() {
        for (HttpStatus status : new HttpStatus[] {
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.UNAUTHORIZED,
        }) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            var service = new LiveFxService(
                builder,
                "test-secret",
                "https://provider.test",
                7,
                clock,
                limiter
            );
            server
                .expect(anything())
                .andRespond(withStatus(status).body("private upstream details"));
            assertThatThrownBy(() -> service.rate("USD", "EUR"))
                .isInstanceOf(ApiException.class)
                .hasMessageNotContaining("private")
                .hasMessageNotContaining("test-secret");
            server.verify();
        }
    }
}
