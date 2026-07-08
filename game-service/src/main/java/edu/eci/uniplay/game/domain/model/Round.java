package edu.eci.uniplay.game.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record Round(
        RoundId id,
        SecretWord secretWord,
        RoundMode mode,
        PlayerId drawerId,
        RoundStatus status,
        Instant startedAt,
        Instant endsAt,
        PlayerId guessedBy,
        Instant finishedAt,
        Map<PlayerId, PlayerId> votes
) {

    public Round {
        if (id == null) {
            throw new IllegalArgumentException("round id is required");
        }
        if (secretWord == null) {
            throw new IllegalArgumentException("secret word is required");
        }
        if (mode == null) {
            throw new IllegalArgumentException("round mode is required");
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
        if (votes == null) {
            votes = Map.of();
        } else {
            votes = Map.copyOf(votes);
        }
    }

    public static Round start(RoundId id, SecretWord secretWord, RoundMode mode, Instant startedAt, Instant endsAt) {
        return start(id, secretWord, mode, null, startedAt, endsAt);
    }

    public static Round start(RoundId id, SecretWord secretWord, RoundMode mode, PlayerId drawerId, Instant startedAt, Instant endsAt) {
        return new Round(id, secretWord, mode, drawerId, RoundStatus.ACTIVE, startedAt, endsAt, null, null, Map.of());
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

    public boolean isDrawnBy(PlayerId playerId) {
        return drawerId != null && drawerId.equals(playerId);
    }

    public Round finish(PlayerId playerId, Instant finishedAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (isExpiredAt(finishedAt)) {
            throw new RoundExpiredException("round " + id.value() + " expired at " + endsAt);
        }

        return new Round(id, secretWord, mode, drawerId, RoundStatus.FINISHED, startedAt, endsAt, playerId, finishedAt, votes);
    }

    public Round expire(Instant expiredAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (!isExpiredAt(expiredAt)) {
            throw new RoundNotExpiredException("round " + id.value() + " has not expired yet");
        }

        return new Round(id, secretWord, mode, drawerId, RoundStatus.EXPIRED, startedAt, endsAt, null, expiredAt, votes);
    }

    public Round castVote(PlayerId voterId, PlayerId candidateId) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (mode != RoundMode.ALL_DRAW) {
            throw new VotingNotEnabledException("round " + id.value() + " does not accept votes");
        }
        if (voterId.equals(candidateId)) {
            throw new SelfVoteException("players cannot vote for themselves");
        }
        if (votes.containsKey(voterId)) {
            throw new DuplicateVoteException("player " + voterId.value() + " already voted in round " + id.value());
        }

        Map<PlayerId, PlayerId> updatedVotes = new LinkedHashMap<>(votes);
        updatedVotes.put(voterId, candidateId);
        return new Round(id, secretWord, mode, drawerId, status, startedAt, endsAt, guessedBy, finishedAt, updatedVotes);
    }

    public Map<PlayerId, Integer> voteTallies() {
        Map<PlayerId, Integer> tallies = new LinkedHashMap<>();
        votes.values().forEach(candidateId -> tallies.merge(candidateId, 1, Integer::sum));
        return Map.copyOf(tallies);
    }
}
