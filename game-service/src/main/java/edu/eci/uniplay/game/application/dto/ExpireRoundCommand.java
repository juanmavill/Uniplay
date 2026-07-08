package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record ExpireRoundCommand(String roomCode, UUID roundId) {
}
