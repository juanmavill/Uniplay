package edu.eci.uniplay.game.domain.model;

import java.time.Instant;

public record Round(
        RoundId id,
        SecretWord secretWord,
        RoundStatus status,
        Instant startedAt,
        PlayerId guessedBy,
        Instant finishedAt
) {

    public Round {
        if (id == null) {
            throw new IllegalArgumentException("round id is required");
        }
        if (secretWord == null) {
            throw new IllegalArgumentException("secret word is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("round status is required");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("round start time is required");
        }
        if (status == RoundStatus.FINISHED && (guessedBy == null || finishedAt == null)) {
            throw new IllegalArgumentException("finished round requires winner and finish time");
        }
    }

    public static Round start(RoundId id, SecretWord secretWord, Instant startedAt) {
        return new Round(id, secretWord, RoundStatus.ACTIVE, startedAt, null, null);
    }

    public boolean isActive() {
        return status == RoundStatus.ACTIVE;
    }

    public boolean matches(String answer) {
        return secretWord.matches(answer);
    }

    public Round finish(PlayerId playerId, Instant finishedAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }

        return new Round(id, secretWord, RoundStatus.FINISHED, startedAt, playerId, finishedAt);
    }
}
