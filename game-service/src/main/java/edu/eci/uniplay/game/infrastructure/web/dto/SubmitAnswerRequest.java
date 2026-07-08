package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
        @NotNull UUID playerId,
        @NotBlank String answer
) {
}
