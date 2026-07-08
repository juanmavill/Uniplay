package edu.eci.uniplay.voice.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VoiceTokenRequest(
        @NotBlank String roomCode,
        @NotBlank String playerId,
        @NotBlank String playerName
) {
}
