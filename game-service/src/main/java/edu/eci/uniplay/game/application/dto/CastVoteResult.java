package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CastVoteResult(
        String roomCode,
        UUID roundId,
        UUID voterId,
        UUID candidateId,
        List<VoteTallyResult> tallies,
        Instant votedAt
) {
}
