package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record SubmitAnswerCommand(
        String roomCode,
        UUID playerId,
        String answer
) {
}
