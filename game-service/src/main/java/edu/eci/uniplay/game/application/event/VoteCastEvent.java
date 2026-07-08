package edu.eci.uniplay.game.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoteCastEvent(
        String roomCode,
        UUID roundId,
        UUID voterId,
        UUID candidateId,
        List<VoteTallyPayload> tallies,
        Instant occurredAt
) {

    public record VoteTallyPayload(UUID candidateId, int votes) {
    }
}
