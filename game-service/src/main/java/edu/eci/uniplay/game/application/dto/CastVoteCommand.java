package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record CastVoteCommand(
        String roomCode,
        UUID roundId,
        UUID voterId,
        UUID candidateId
) {
}
