package edu.eci.uniplay.game.application.event;

import java.time.Instant;
import java.util.UUID;

public record RoundStartedEvent(
        String roomCode,
        UUID roundId,
        String word,
        Instant startedAt,
        Instant endsAt,
        Instant occurredAt
) {
}
