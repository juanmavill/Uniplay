package edu.eci.uniplay.game.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.StartRoundResult;

public record StartRoundResponse(
        String roomCode,
        UUID roundId,
        String word,
        UUID drawerId,
        String mode,
        String deck,
        String status,
        Instant startedAt,
        Instant endsAt
) {

    public static StartRoundResponse from(StartRoundResult result) {
        return new StartRoundResponse(
                result.roomCode(),
                result.roundId(),
                result.word(),
                result.drawerId(),
                result.mode(),
                result.deck(),
                result.status(),
                result.startedAt(),
                result.endsAt()
        );
    }
}
