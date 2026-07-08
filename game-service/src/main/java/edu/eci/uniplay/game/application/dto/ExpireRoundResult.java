package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ExpireRoundResult(
        String roomCode,
        UUID roundId,
        String status,
        String reason,
        Instant finishedAt
) {
}
