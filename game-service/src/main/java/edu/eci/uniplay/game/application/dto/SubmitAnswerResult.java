package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmitAnswerResult(
        String roomCode,
        UUID roundId,
        UUID playerId,
        boolean correct,
        int score,
        String roundStatus,
        Instant answeredAt
) {
}
