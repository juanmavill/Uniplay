package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.CastVoteResult;

public record CastVoteResponse(
        String roomCode,
        UUID roundId,
        UUID voterId,
        UUID candidateId,
        List<VoteTallyResponse> tallies,
        Instant votedAt
) {

    public static CastVoteResponse from(CastVoteResult result) {
        return new CastVoteResponse(
                result.roomCode(),
                result.roundId(),
                result.voterId(),
                result.candidateId(),
                result.tallies().stream().map(VoteTallyResponse::from).toList(),
                result.votedAt()
        );
    }
}
