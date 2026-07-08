package edu.eci.uniplay.voice.infrastructure.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "uniplay.voice")
public record VoiceProperties(
        @NotBlank String livekitUrl,
        @NotBlank String publicUrl,
        @NotBlank String apiKey,
        @NotBlank String apiSecret,
        @NotNull Duration tokenTtl
) {
}
