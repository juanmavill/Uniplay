package edu.eci.uniplay.gateway.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uniplay.gateway")
public class GatewayServiceProperties {

    private URI roomServiceUri = URI.create("http://localhost:8081");
    private URI gameServiceUri = URI.create("http://localhost:8082");
    private URI realtimeServiceUri = URI.create("http://localhost:8083");
    private URI metricsServiceUri = URI.create("http://localhost:8084");
    private URI voiceServiceUri = URI.create("http://localhost:8085");
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
    private RateLimit rateLimit = new RateLimit();

    public URI getRoomServiceUri() {
        return roomServiceUri;
    }

    public void setRoomServiceUri(URI roomServiceUri) {
        this.roomServiceUri = roomServiceUri;
    }

    public URI getGameServiceUri() {
        return gameServiceUri;
    }

    public void setGameServiceUri(URI gameServiceUri) {
        this.gameServiceUri = gameServiceUri;
    }

    public URI getRealtimeServiceUri() {
        return realtimeServiceUri;
    }

    public void setRealtimeServiceUri(URI realtimeServiceUri) {
        this.realtimeServiceUri = realtimeServiceUri;
    }

    public URI getMetricsServiceUri() {
        return metricsServiceUri;
    }

    public void setMetricsServiceUri(URI metricsServiceUri) {
        this.metricsServiceUri = metricsServiceUri;
    }

    public URI getVoiceServiceUri() {
        return voiceServiceUri;
    }

    public void setVoiceServiceUri(URI voiceServiceUri) {
        this.voiceServiceUri = voiceServiceUri;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class RateLimit {

        private boolean enabled = true;
        private int requestsPerWindow = 120;
        private Duration window = Duration.ofMinutes(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerWindow() {
            return requestsPerWindow;
        }

        public void setRequestsPerWindow(int requestsPerWindow) {
            this.requestsPerWindow = requestsPerWindow;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
