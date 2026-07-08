package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;

public record SubmitAnswerResponse(
        String roomCode,
        UUID roundId,
        UUID playerId,
        boolean correct,
        int score,
        String roundStatus,
        Instant answeredAt
) {

    public static SubmitAnswerResponse from(SubmitAnswerResult result) {
        return new SubmitAnswerResponse(
                result.roomCode(),
                result.roundId(),
                result.playerId(),
                result.correct(),
                result.score(),
                result.roundStatus(),
                result.answeredAt()
        );
    }
}
