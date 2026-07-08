package edu.eci.uniplay.room.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uniplay.room")
public record RoomProperties(
        int maxPlayers,
        int codeGenerationMaxAttempts,
        Duration ttl
) {

    public RoomProperties {
        if (maxPlayers < 2) {
            throw new IllegalArgumentException("maxPlayers must be at least 2");
        }

        if (codeGenerationMaxAttempts < 1) {
            throw new IllegalArgumentException("codeGenerationMaxAttempts must be at least 1");
        }

        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }
}
