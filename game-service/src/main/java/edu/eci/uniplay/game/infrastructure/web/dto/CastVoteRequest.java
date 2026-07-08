package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CastVoteRequest(
        @NotNull UUID voterId,
        @NotNull UUID candidateId
) {
}
