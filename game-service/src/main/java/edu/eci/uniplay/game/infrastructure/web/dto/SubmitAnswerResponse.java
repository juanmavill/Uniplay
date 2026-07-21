package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;

public record SubmitAnswerResponse(
        String roomCode,
        UUID roundId,
        UUID playerId,
        boolean correct,
        boolean newlyGuessed,
        int score,
        int pointsAwarded,
        boolean drawerBonusAwarded,
        String roundStatus,
        Instant answeredAt
) {

    public static SubmitAnswerResponse from(SubmitAnswerResult result) {
        return new SubmitAnswerResponse(
                result.roomCode(),
                result.roundId(),
                result.playerId(),
                result.correct(),
                result.newlyGuessed(),
                result.score(),
                result.pointsAwarded(),
                result.drawerBonusAwarded(),
                result.roundStatus(),
                result.answeredAt()
        );
    }
}
