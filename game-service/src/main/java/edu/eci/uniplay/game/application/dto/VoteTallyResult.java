package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record VoteTallyResult(UUID candidateId, int votes) {
}
