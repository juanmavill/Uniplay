package edu.eci.uniplay.game.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "uniplay.game")
public record GameProperties(
        @Min(1) int pointsPerCorrectAnswer,
        @NotNull Duration sessionTtl
) {
}
