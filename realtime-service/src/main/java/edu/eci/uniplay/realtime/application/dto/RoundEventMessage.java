package edu.eci.uniplay.realtime.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoundEventMessage(
        String type,
        String roomCode,
        UUID roundId,
        String word,
        String status,
        String reason,
        UUID playerId,
        UUID voterId,
        UUID candidateId,
        Integer score,
        List<VoteTallyMessage> tallies,
        Instant startedAt,
        Instant endsAt,
        Instant finishedAt,
        Instant occurredAt
) {
    public record VoteTallyMessage(UUID candidateId, int votes) {
    }
}
