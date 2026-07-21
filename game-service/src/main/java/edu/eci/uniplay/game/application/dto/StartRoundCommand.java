package edu.eci.uniplay.game.application.dto;

import java.util.List;
import java.util.UUID;

public record StartRoundCommand(
        String roomCode,
        String mode,
        String deck,
        UUID drawerId,
        List<String> customWords,
        List<UUID> participantIds
) {
}
