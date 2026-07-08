package edu.eci.uniplay.game.infrastructure.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisGameSessionRepositoryTest {

    private static final Duration TTL = Duration.ofHours(2);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisGameSessionRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new RedisGameSessionRepository(redisTemplate, new ObjectMapper(), TTL);
    }

    @Test
    void savesSessionWithTtl() {
        GameSession session = GameSession.newFor(new RoomCode("ABC123"))
                .startRound(
                        new RoundId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                        new SecretWord("Campus"),
                        Instant.parse("2026-07-07T12:00:00Z"),
                        Instant.parse("2026-07-07T12:01:00Z")
                );

        repository.save(session);

        verify(valueOperations).set(
                eq("game-session:ABC123"),
                contains("\"word\":\"Campus\""),
                eq(TTL)
        );
    }

    @Test
    void findsSessionByRoomCode() {
        when(valueOperations.get("game-session:ABC123")).thenReturn(sessionJson());

        Optional<GameSession> foundSession = repository.findByRoomCode(new RoomCode("ABC123"));

        assertThat(foundSession).isPresent();
        assertThat(foundSession.get().round()).get().satisfies(round -> {
            assertThat(round.secretWord()).isEqualTo(new SecretWord("Campus"));
            assertThat(round.guessedBy()).isEqualTo(new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222")));
        });
        assertThat(foundSession.get().scoreOf(new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"))))
                .isEqualTo(100);
    }

    @Test
    void returnsEmptyWhenSessionDoesNotExist() {
        when(valueOperations.get("game-session:ABC123")).thenReturn(null);

        Optional<GameSession> foundSession = repository.findByRoomCode(new RoomCode("ABC123"));

        assertThat(foundSession).isEmpty();
    }

    @Test
    void savesSessionWithoutRound() {
        GameSession session = GameSession.restore(new RoomCode("ABC123"), null, Map.of());

        repository.save(session);

        verify(valueOperations).set(
                eq("game-session:ABC123"),
                contains("\"round\":null"),
                eq(TTL)
        );
    }

    private String sessionJson() {
        return """
                {
                  "roomCode": "ABC123",
                  "round": {
                    "id": "11111111-1111-1111-1111-111111111111",
                    "word": "Campus",
                    "status": "FINISHED",
                    "startedAt": "2026-07-07T12:00:00Z",
                    "endsAt": "2026-07-07T12:01:00Z",
                    "guessedBy": "22222222-2222-2222-2222-222222222222",
                    "finishedAt": "2026-07-07T12:00:10Z"
                  },
                  "scores": [
                    {
                      "playerId": "22222222-2222-2222-2222-222222222222",
                      "score": 100
                    }
                  ]
                }
                """;
    }
}
