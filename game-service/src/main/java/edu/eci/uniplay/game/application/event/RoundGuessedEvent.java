package edu.eci.uniplay.game.application.event;

import java.time.Instant;
import java.util.UUID;

public record RoundGuessedEvent(
        String roomCode,
        UUID roundId,
        UUID playerId,
        int score,
        Instant occurredAt
) {
}
