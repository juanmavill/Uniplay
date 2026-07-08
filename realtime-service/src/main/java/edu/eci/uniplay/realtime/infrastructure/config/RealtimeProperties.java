package edu.eci.uniplay.realtime.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uniplay.realtime")
public record RealtimeProperties(String websocketEndpoint, List<String> allowedOrigins) {

    public RealtimeProperties {
        if (websocketEndpoint == null || websocketEndpoint.isBlank()) {
            throw new IllegalArgumentException("websocketEndpoint is required");
        }

        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("*");
        }
    }
}
