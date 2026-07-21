package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import edu.eci.uniplay.game.application.dto.RoundResult;

public record RoundResponse(
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

    static RoundResponse from(RoundResult result) {
        return new RoundResponse(
                result.roundId(),
                result.mode(),
                result.status(),
                result.word(),
                result.drawerId(),
                result.guessedBy(),
                result.guessedPlayerIds(),
                result.eligibleGuesserCount(),
                result.startedAt(),
                result.endsAt(),
                result.finishedAt()
        );
    }
}
