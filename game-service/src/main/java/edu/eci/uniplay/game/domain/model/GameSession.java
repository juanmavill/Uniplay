package edu.eci.uniplay.game.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GameSession {

    private final RoomCode roomCode;
    private final Round round;
    private final Map<PlayerId, Integer> scores;

    private GameSession(RoomCode roomCode, Round round, Map<PlayerId, Integer> scores) {
        this.roomCode = roomCode;
        this.round = round;
        this.scores = Map.copyOf(scores);
    }

    public static GameSession newFor(RoomCode roomCode) {
        return new GameSession(roomCode, null, Map.of());
    }

    public static GameSession restore(RoomCode roomCode, Round round, Map<PlayerId, Integer> scores) {
        return new GameSession(roomCode, round, scores);
    }

    public GameSession startRound(RoundId roundId, SecretWord secretWord, RoundMode mode, Instant startedAt, Instant endsAt) {
        if (round != null && round.isActive()) {
            throw new RoundAlreadyActiveException("room " + roomCode.value() + " already has an active round");
        }

        return new GameSession(roomCode, Round.start(roundId, secretWord, mode, startedAt, endsAt), scores);
    }

    public AnswerEvaluation submitAnswer(PlayerId playerId, String answer, int points, Instant answeredAt) {
        if (round == null || !round.isActive()) {
            throw new RoundNotActiveException("room " + roomCode.value() + " does not have an active round");
        }

        int currentScore = scoreOf(playerId);

        if (!round.matches(answer)) {
            return new AnswerEvaluation(this, false, currentScore, round.id());
        }

        Map<PlayerId, Integer> updatedScores = new LinkedHashMap<>(scores);
        int newScore = currentScore + points;
        updatedScores.put(playerId, newScore);

        GameSession updatedSession = new GameSession(roomCode, round.finish(playerId, answeredAt), updatedScores);
        return new AnswerEvaluation(updatedSession, true, newScore, round.id());
    }

    public GameSession expireRound(RoundId roundId, Instant expiredAt) {
        if (round == null || !round.isActive() || !round.id().equals(roundId)) {
            throw new RoundNotActiveException("room " + roomCode.value() + " does not have round " + roundId.value() + " active");
        }

        return new GameSession(roomCode, round.expire(expiredAt), scores);
    }

    public VoteEvaluation castVote(RoundId roundId, PlayerId voterId, PlayerId candidateId) {
        if (round == null || !round.isActive() || !round.id().equals(roundId)) {
            throw new RoundNotActiveException("room " + roomCode.value() + " does not have round " + roundId.value() + " active");
        }

        Round updatedRound = round.castVote(voterId, candidateId);
        GameSession updatedSession = new GameSession(roomCode, updatedRound, scores);
        return new VoteEvaluation(updatedSession, roundId, voterId, candidateId, updatedRound.voteTallies());
    }

    public int scoreOf(PlayerId playerId) {
        return scores.getOrDefault(playerId, 0);
    }

    public RoomCode roomCode() {
        return roomCode;
    }

    public Optional<Round> round() {
        return Optional.ofNullable(round);
    }

    public Map<PlayerId, Integer> scores() {
        return scores;
    }
}
