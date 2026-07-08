package edu.eci.uniplay.gateway.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import edu.eci.uniplay.gateway.infrastructure.config.GatewayServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class BasicRateLimitingFilterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-08T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsRequestsAfterClientExceedsConfiguredWindow() {
        BasicRateLimitingFilter filter = new BasicRateLimitingFilter(propertiesWithLimit(2), FIXED_CLOCK);
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.filter(exchangeFor("/salas", "203.0.113.10"), chain).block();
        filter.filter(exchangeFor("/games/ABC123", "203.0.113.10"), chain).block();
        MockServerWebExchange rejectedExchange = exchangeFor("/voice/token", "203.0.113.10");
        filter.filter(rejectedExchange, chain).block();

        assertThat(chain.calls()).isEqualTo(2);
        assertThat(rejectedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rejectedExchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void skipsManagementAndPreflightRequests() {
        BasicRateLimitingFilter filter = new BasicRateLimitingFilter(propertiesWithLimit(1), FIXED_CLOCK);
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.filter(exchangeFor("/actuator/health", "198.51.100.20"), chain).block();
        filter.filter(optionsExchangeFor("/salas", "198.51.100.20"), chain).block();

        assertThat(chain.calls()).isEqualTo(2);
    }

    @Test
    void canBeDisabledByConfiguration() {
        GatewayServiceProperties properties = propertiesWithLimit(0);
        properties.getRateLimit().setEnabled(false);
        BasicRateLimitingFilter filter = new BasicRateLimitingFilter(properties, FIXED_CLOCK);
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.filter(exchangeFor("/salas", "192.0.2.30"), chain).block();

        assertThat(chain.calls()).isEqualTo(1);
    }

    private static GatewayServiceProperties propertiesWithLimit(int limit) {
        GatewayServiceProperties properties = new GatewayServiceProperties();
        properties.getRateLimit().setRequestsPerWindow(limit);
        properties.getRateLimit().setWindow(Duration.ofMinutes(1));
        return properties;
    }

    private static MockServerWebExchange exchangeFor(String path, String clientIp) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
                .header("X-Forwarded-For", clientIp)
                .build();
        return MockServerWebExchange.from(request);
    }

    private static MockServerWebExchange optionsExchangeFor(String path, String clientIp) {
        MockServerHttpRequest request = MockServerHttpRequest.options(path)
                .header("X-Forwarded-For", clientIp)
                .build();
        return MockServerWebExchange.from(request);
    }

    private static final class RecordingFilterChain implements GatewayFilterChain {

        private int calls;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            calls++;
            return Mono.empty();
        }

        private int calls() {
            return calls;
        }
    }
}
