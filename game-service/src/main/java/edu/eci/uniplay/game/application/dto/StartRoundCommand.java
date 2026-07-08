package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record StartRoundCommand(String roomCode, String mode, String deck, UUID drawerId) {
}
