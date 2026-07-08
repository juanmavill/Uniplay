package edu.eci.uniplay.room.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.eci.uniplay.room.application.dto.JoinRoomResult;

public record JoinRoomResponse(
        UUID roomId,
        String code,
        UUID playerId,
        String playerName,
        List<PlayerResponse> players,
        Instant joinedAt
) {

    public static JoinRoomResponse from(JoinRoomResult result) {
        return new JoinRoomResponse(
                result.roomId(),
                result.code(),
                result.playerId(),
                result.playerName(),
                result.players().stream().map(PlayerResponse::from).toList(),
                result.joinedAt()
        );
    }
}
