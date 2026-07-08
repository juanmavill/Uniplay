package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.RoundResult;

public record RoundResponse(
        UUID roundId,
        String status,
        String word,
        UUID guessedBy,
        Instant startedAt,
        Instant finishedAt
) {

    static RoundResponse from(RoundResult result) {
        return new RoundResponse(
                result.roundId(),
                result.status(),
                result.word(),
                result.guessedBy(),
                result.startedAt(),
                result.finishedAt()
        );
    }
}
