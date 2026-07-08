package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.ExpireRoundResult;

public record ExpireRoundResponse(
        String roomCode,
        UUID roundId,
        String status,
        String reason,
        Instant finishedAt
) {

    public static ExpireRoundResponse from(ExpireRoundResult result) {
        return new ExpireRoundResponse(
                result.roomCode(),
                result.roundId(),
                result.status(),
                result.reason(),
                result.finishedAt()
        );
    }
}
