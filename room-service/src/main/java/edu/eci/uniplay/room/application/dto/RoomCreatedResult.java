package edu.eci.uniplay.room.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomCreatedResult(
        UUID roomId,
        String code,
        String status,
        int maxPlayers,
        Instant createdAt
) {
}
