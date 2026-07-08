package edu.eci.uniplay.room.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

import edu.eci.uniplay.room.application.dto.ListPlayersResult;

public record ListPlayersResponse(UUID roomId, String code, List<PlayerResponse> players) {

    public static ListPlayersResponse from(ListPlayersResult result) {
        return new ListPlayersResponse(
                result.roomId(),
                result.code(),
                result.players().stream().map(PlayerResponse::from).toList()
        );
    }
}
