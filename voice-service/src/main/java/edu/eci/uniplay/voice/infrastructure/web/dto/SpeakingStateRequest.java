package edu.eci.uniplay.voice.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpeakingStateRequest(
        @NotBlank String roomCode,
        @NotBlank String playerId,
        @NotNull Boolean speaking
) {
}
