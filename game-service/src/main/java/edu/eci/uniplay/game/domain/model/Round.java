package edu.eci.uniplay.game.domain.model;

import java.time.Instant;

public record Round(
        RoundId id,
        SecretWord secretWord,
        RoundStatus status,
        Instant startedAt,
        Instant endsAt,
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
        if (endsAt == null) {
            throw new IllegalArgumentException("round end time is required");
        }
        if (!endsAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("round end time must be after start time");
        }
        if (status == RoundStatus.FINISHED && (guessedBy == null || finishedAt == null)) {
            throw new IllegalArgumentException("finished round requires winner and finish time");
        }
        if (status == RoundStatus.EXPIRED && finishedAt == null) {
            throw new IllegalArgumentException("expired round requires finish time");
        }
    }

    public static Round start(RoundId id, SecretWord secretWord, Instant startedAt, Instant endsAt) {
        return new Round(id, secretWord, RoundStatus.ACTIVE, startedAt, endsAt, null, null);
    }

    public boolean isActive() {
        return status == RoundStatus.ACTIVE;
    }

    public boolean isExpiredAt(Instant instant) {
        return isActive() && !instant.isBefore(endsAt);
    }

    public boolean matches(String answer) {
        return secretWord.matches(answer);
    }

    public Round finish(PlayerId playerId, Instant finishedAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (isExpiredAt(finishedAt)) {
            throw new RoundExpiredException("round " + id.value() + " expired at " + endsAt);
        }

        return new Round(id, secretWord, RoundStatus.FINISHED, startedAt, endsAt, playerId, finishedAt);
    }

    public Round expire(Instant expiredAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (!isExpiredAt(expiredAt)) {
            throw new RoundNotExpiredException("round " + id.value() + " has not expired yet");
        }

        return new Round(id, secretWord, RoundStatus.EXPIRED, startedAt, endsAt, null, expiredAt);
    }
}
