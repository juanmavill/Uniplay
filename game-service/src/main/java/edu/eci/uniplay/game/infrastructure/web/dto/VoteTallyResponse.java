package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.UUID;

import edu.eci.uniplay.game.application.dto.VoteTallyResult;

public record VoteTallyResponse(UUID candidateId, int votes) {

    static VoteTallyResponse from(VoteTallyResult result) {
        return new VoteTallyResponse(result.candidateId(), result.votes());
    }
}
