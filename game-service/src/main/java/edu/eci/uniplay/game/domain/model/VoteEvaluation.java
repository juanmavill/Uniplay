package edu.eci.uniplay.game.domain.model;

import java.util.Map;

public record VoteEvaluation(
        GameSession session,
        RoundId roundId,
        PlayerId voterId,
        PlayerId candidateId,
        Map<PlayerId, Integer> tallies
) {
}
