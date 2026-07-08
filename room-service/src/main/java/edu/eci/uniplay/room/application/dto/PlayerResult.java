package edu.eci.uniplay.room.application.dto;

import java.util.UUID;

import edu.eci.uniplay.room.domain.model.Player;

public record PlayerResult(UUID playerId, String playerName) {

    public static PlayerResult from(Player player) {
        return new PlayerResult(player.id().value(), player.name().value());
    }
}
