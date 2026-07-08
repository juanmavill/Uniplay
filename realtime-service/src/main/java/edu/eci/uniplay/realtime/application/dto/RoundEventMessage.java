package edu.eci.uniplay.realtime.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RoundEventMessage(
        String type,
        String roomCode,
        UUID roundId,
        String word,
        String status,
        String reason,
        UUID playerId,
        Integer score,
        Instant startedAt,
        Instant endsAt,
        Instant finishedAt,
        Instant occurredAt
) {
}
