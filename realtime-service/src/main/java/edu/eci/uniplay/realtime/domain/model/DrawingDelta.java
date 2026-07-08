package edu.eci.uniplay.realtime.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DrawingDelta(
        String roomCode,
        UUID playerId,
        CanvasPoint from,
        CanvasPoint to,
        StrokeStyle strokeStyle,
        Instant occurredAt
) {

    public DrawingDelta {
        roomCode = normalizeRoomCode(roomCode);
        Objects.requireNonNull(playerId, "playerId is required");
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(strokeStyle, "strokeStyle is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    private static String normalizeRoomCode(String roomCode) {
        Objects.requireNonNull(roomCode, "roomCode is required");
        String normalizedRoomCode = roomCode.trim().toUpperCase();

        if (!normalizedRoomCode.matches("[A-Z0-9]{6}")) {
            throw new InvalidDrawingDeltaException("roomCode must contain 6 uppercase alphanumeric characters");
        }

        return normalizedRoomCode;
    }
}
