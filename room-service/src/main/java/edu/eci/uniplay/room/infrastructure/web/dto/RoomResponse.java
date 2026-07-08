package edu.eci.uniplay.room.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.room.application.dto.RoomCreatedResult;

public record RoomResponse(
        UUID roomId,
        String code,
        String status,
        int maxPlayers,
        Instant createdAt
) {

    public static RoomResponse from(RoomCreatedResult result) {
        return new RoomResponse(
                result.roomId(),
                result.code(),
                result.status(),
                result.maxPlayers(),
                result.createdAt()
        );
    }
}
