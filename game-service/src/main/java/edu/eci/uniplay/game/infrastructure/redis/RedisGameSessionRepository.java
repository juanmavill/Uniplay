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
            String status,
            String startedAt,
            String guessedBy,
            String finishedAt
    ) {

        static RoundDocument from(Round round) {
            return new RoundDocument(
                    round.id().value().toString(),
                    round.secretWord().value(),
                    round.status().name(),
                    round.startedAt().toString(),
                    round.guessedBy() == null ? null : round.guessedBy().value().toString(),
                    round.finishedAt() == null ? null : round.finishedAt().toString()
            );
        }

        Round toDomain() {
            return new Round(
                    new RoundId(UUID.fromString(id)),
                    new SecretWord(word),
                    RoundStatus.valueOf(status),
                    Instant.parse(startedAt),
                    guessedBy == null ? null : new PlayerId(UUID.fromString(guessedBy)),
                    finishedAt == null ? null : Instant.parse(finishedAt)
            );
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
