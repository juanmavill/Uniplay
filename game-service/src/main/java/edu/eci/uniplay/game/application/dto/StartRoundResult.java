package edu.eci.uniplay.game.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StartRoundResult(
        String roomCode,
        UUID roundId,
        String word,
        String mode,
        String status,
        Instant startedAt,
        Instant endsAt
) {
}
