package edu.eci.uniplay.room.infrastructure.web.dto;

import java.util.UUID;

import edu.eci.uniplay.room.application.dto.PlayerResult;

public record PlayerResponse(UUID playerId, String playerName) {

    public static PlayerResponse from(PlayerResult result) {
        return new PlayerResponse(result.playerId(), result.playerName());
    }
}
