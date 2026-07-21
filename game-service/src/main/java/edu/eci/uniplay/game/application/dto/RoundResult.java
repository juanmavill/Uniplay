package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record RoundResult(
        UUID roundId,
        String mode,
        String status,
        String word,
        UUID drawerId,
        UUID guessedBy,
        List<UUID> guessedPlayerIds,
        int eligibleGuesserCount,
        Instant startedAt,
        Instant endsAt,
        Instant finishedAt
) {
}
