package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmitAnswerResult(
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
}
