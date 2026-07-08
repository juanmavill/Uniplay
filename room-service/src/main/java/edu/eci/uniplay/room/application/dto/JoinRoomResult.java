package edu.eci.uniplay.room.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JoinRoomResult(
        UUID roomId,
        String code,
        UUID playerId,
        String playerName,
        List<PlayerResult> players,
        Instant joinedAt
) {
}
