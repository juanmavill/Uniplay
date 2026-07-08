package edu.eci.uniplay.gateway.infrastructure.ratelimit;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.eci.uniplay.gateway.infrastructure.config.GatewayServiceProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BasicRateLimitingFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -100;
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final String FORWARDED_FOR_SEPARATOR = ",";

    private final GatewayServiceProperties properties;
    private final Clock clock;
    private final Map<String, WindowCounter> countersByClient = new ConcurrentHashMap<>();

    public BasicRateLimitingFilter(GatewayServiceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayServiceProperties.RateLimit rateLimit = properties.getRateLimit();
        if (!rateLimit.isEnabled() || isManagementOrPreflight(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        RateLimitDecision decision = decide(clientKey(exchange.getRequest()), rateLimit);
        addRateLimitHeaders(exchange, rateLimit, decision);
        if (decision.allowed()) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    private RateLimitDecision decide(String clientKey, GatewayServiceProperties.RateLimit rateLimit) {
        long now = clock.millis();
        WindowCounter counter = countersByClient.computeIfAbsent(
                clientKey,
                ignored -> new WindowCounter(now, 0)
        );

        synchronized (counter) {
            long windowMillis = rateLimit.getWindow().toMillis();
            if (now - counter.windowStartedAt() >= windowMillis) {
                counter.reset(now);
            }

            if (counter.requests() >= rateLimit.getRequestsPerWindow()) {
                return new RateLimitDecision(false, 0);
            }

            counter.increment();
            return new RateLimitDecision(true, rateLimit.getRequestsPerWindow() - counter.requests());
        }
    }

    private boolean isManagementOrPreflight(ServerHttpRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod().name())
                || request.getPath().pathWithinApplication().value().startsWith("/actuator");
    }

    private String clientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(FORWARDED_FOR_SEPARATOR)[0].trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT;
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private void addRateLimitHeaders(
            ServerWebExchange exchange,
            GatewayServiceProperties.RateLimit rateLimit,
            RateLimitDecision decision
    ) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(rateLimit.getRequestsPerWindow()));
        headers.add("X-RateLimit-Remaining", String.valueOf(decision.remainingRequests()));
    }

    private record RateLimitDecision(boolean allowed, int remainingRequests) {
    }

    private static final class WindowCounter {

        private long windowStartedAt;
        private int requests;

        private WindowCounter(long windowStartedAt, int requests) {
            this.windowStartedAt = windowStartedAt;
            this.requests = requests;
        }

        private long windowStartedAt() {
            return windowStartedAt;
        }

        private int requests() {
            return requests;
        }

        private void increment() {
            requests++;
        }

        private void reset(long newWindowStartedAt) {
            windowStartedAt = newWindowStartedAt;
            requests = 0;
        }
    }
}
