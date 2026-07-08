package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RoundResult(
        UUID roundId,
        String status,
        String word,
        UUID guessedBy,
        Instant startedAt,
        Instant finishedAt
) {
}
