package edu.eci.uniplay.game.application.event;

import java.time.Instant;
import java.util.UUID;

public record RoundFinishedEvent(
        String roomCode,
        UUID roundId,
        String status,
        String reason,
        Instant finishedAt,
        Instant occurredAt
) {
}
