package edu.eci.uniplay.game.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
        Set<PlayerId> eligibleGuessers,
        Set<PlayerId> guessedPlayers,
        boolean drawerBonusAwarded,
        Map<PlayerId, PlayerId> votes
) {

    public Round(
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
        this(
                id, secretWord, mode, drawerId, status, startedAt, endsAt, guessedBy, finishedAt,
                Set.of(), guessedBy == null ? Set.of() : Set.of(guessedBy), false, votes
        );
    }

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
        eligibleGuessers = eligibleGuessers == null ? Set.of() : Set.copyOf(eligibleGuessers);
        guessedPlayers = guessedPlayers == null ? Set.of() : Set.copyOf(guessedPlayers);
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
        return start(id, secretWord, mode, drawerId, Set.of(), startedAt, endsAt);
    }

    public static Round start(
            RoundId id,
            SecretWord secretWord,
            RoundMode mode,
            PlayerId drawerId,
            Set<PlayerId> eligibleGuessers,
            Instant startedAt,
            Instant endsAt
    ) {
        return new Round(
                id, secretWord, mode, drawerId, RoundStatus.ACTIVE, startedAt, endsAt, null, null,
                eligibleGuessers, Set.of(), false, Map.of()
        );
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

    public boolean hasGuessed(PlayerId playerId) {
        return guessedPlayers.contains(playerId);
    }

    public boolean hasMajorityGuessed() {
        return !eligibleGuessers.isEmpty() && guessedPlayers.size() > eligibleGuessers.size() / 2;
    }

    public Round markDrawerBonusAwarded() {
        if (drawerBonusAwarded) {
            return this;
        }
        return new Round(
                id, secretWord, mode, drawerId, status, startedAt, endsAt, guessedBy, finishedAt,
                eligibleGuessers, guessedPlayers, true, votes
        );
    }

    public Round registerCorrectGuess(PlayerId playerId, Instant answeredAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (isExpiredAt(answeredAt)) {
            throw new RoundExpiredException("round " + id.value() + " expired at " + endsAt);
        }
        if (hasGuessed(playerId)) {
            return this;
        }

        Set<PlayerId> updatedGuesses = new java.util.LinkedHashSet<>(guessedPlayers);
        updatedGuesses.add(playerId);
        boolean allGuessed = !eligibleGuessers.isEmpty() && updatedGuesses.containsAll(eligibleGuessers);
        return new Round(
                id, secretWord, mode, drawerId,
                allGuessed ? RoundStatus.FINISHED : RoundStatus.ACTIVE,
                startedAt, endsAt, guessedBy == null ? playerId : guessedBy,
                allGuessed ? answeredAt : null,
                eligibleGuessers, updatedGuesses, drawerBonusAwarded, votes
        );
    }

    public Round expire(Instant expiredAt) {
        if (!isActive()) {
            throw new RoundNotActiveException("round " + id.value() + " is not active");
        }
        if (!isExpiredAt(expiredAt)) {
            throw new RoundNotExpiredException("round " + id.value() + " has not expired yet");
        }

        return new Round(
                id, secretWord, mode, drawerId, RoundStatus.EXPIRED, startedAt, endsAt, guessedBy, expiredAt,
                eligibleGuessers, guessedPlayers, drawerBonusAwarded, votes
        );
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
        return new Round(
                id, secretWord, mode, drawerId, status, startedAt, endsAt, guessedBy, finishedAt,
                eligibleGuessers, guessedPlayers, drawerBonusAwarded, updatedVotes
        );
    }

    public Map<PlayerId, Integer> voteTallies() {
        Map<PlayerId, Integer> tallies = new LinkedHashMap<>();
        votes.values().forEach(candidateId -> tallies.merge(candidateId, 1, Integer::sum));
        return Map.copyOf(tallies);
    }
}
