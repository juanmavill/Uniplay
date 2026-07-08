package edu.eci.uniplay.game.infrastructure.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.RoundMode;
import edu.eci.uniplay.game.domain.model.RoundStatus;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisGameSessionRepository implements GameSessionRepository {

    private static final String GAME_SESSION_KEY_PREFIX = "game-session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisGameSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<GameSession> findByRoomCode(RoomCode roomCode) {
        String sessionJson = redisTemplate.opsForValue().get(sessionKey(roomCode));

        if (sessionJson == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(sessionJson, GameSessionDocument.class).toDomain());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("game session could not be deserialized", exception);
        }
    }

    @Override
    public void save(GameSession session) {
        try {
            redisTemplate.opsForValue().set(
                    sessionKey(session.roomCode()),
                    objectMapper.writeValueAsString(GameSessionDocument.from(session)),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("game session could not be serialized", exception);
        }
    }

    private String sessionKey(RoomCode roomCode) {
        return GAME_SESSION_KEY_PREFIX + roomCode.value();
    }

    private record GameSessionDocument(
            String roomCode,
            RoundDocument round,
            List<ScoreDocument> scores
    ) {

        static GameSessionDocument from(GameSession session) {
            return new GameSessionDocument(
                    session.roomCode().value(),
                    session.round().map(RoundDocument::from).orElse(null),
                    session.scores().entrySet().stream()
                            .map(entry -> ScoreDocument.from(entry.getKey(), entry.getValue()))
                            .toList()
            );
        }

        GameSession toDomain() {
            Map<PlayerId, Integer> restoredScores = new LinkedHashMap<>();
            scores.forEach(score -> restoredScores.put(score.toPlayerId(), score.score()));

            return GameSession.restore(
                    new RoomCode(roomCode),
                    round == null ? null : round.toDomain(),
                    restoredScores
            );
        }
    }

    private record RoundDocument(
            String id,
            String word,
            String mode,
            String status,
            String startedAt,
            String endsAt,
            String guessedBy,
            String finishedAt,
            List<VoteDocument> votes
    ) {

        static RoundDocument from(Round round) {
            return new RoundDocument(
                    round.id().value().toString(),
                    round.secretWord().value(),
                    round.mode().name(),
                    round.status().name(),
                    round.startedAt().toString(),
                    round.endsAt().toString(),
                    round.guessedBy() == null ? null : round.guessedBy().value().toString(),
                    round.finishedAt() == null ? null : round.finishedAt().toString(),
                    round.votes().entrySet().stream()
                            .map(entry -> VoteDocument.from(entry.getKey(), entry.getValue()))
                            .toList()
            );
        }

        Round toDomain() {
            Map<PlayerId, PlayerId> restoredVotes = new LinkedHashMap<>();
            if (votes != null) {
                votes.forEach(vote -> restoredVotes.put(vote.toVoterId(), vote.toCandidateId()));
            }

            return new Round(
                    new RoundId(UUID.fromString(id)),
                    new SecretWord(word),
                    mode == null ? RoundMode.CLASSIC : RoundMode.valueOf(mode),
                    RoundStatus.valueOf(status),
                    Instant.parse(startedAt),
                    Instant.parse(endsAt),
                    guessedBy == null ? null : new PlayerId(UUID.fromString(guessedBy)),
                    finishedAt == null ? null : Instant.parse(finishedAt),
                    restoredVotes
            );
        }
    }

    private record VoteDocument(String voterId, String candidateId) {

        static VoteDocument from(PlayerId voterId, PlayerId candidateId) {
            return new VoteDocument(voterId.value().toString(), candidateId.value().toString());
        }

        PlayerId toVoterId() {
            return new PlayerId(UUID.fromString(voterId));
        }

        PlayerId toCandidateId() {
            return new PlayerId(UUID.fromString(candidateId));
        }
    }

    private record ScoreDocument(String playerId, int score) {

        static ScoreDocument from(PlayerId playerId, int score) {
            return new ScoreDocument(playerId.value().toString(), score);
        }

        PlayerId toPlayerId() {
            return new PlayerId(UUID.fromString(playerId));
        }
    }
}
