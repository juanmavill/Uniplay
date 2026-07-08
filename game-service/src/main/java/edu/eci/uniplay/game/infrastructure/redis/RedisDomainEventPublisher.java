package edu.eci.uniplay.game.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisDomainEventPublisher implements DomainEventPublisher {

    static final String ROUND_STARTED_CHANNEL = "ronda.iniciada";
    static final String ROUND_GUESSED_CHANNEL = "palabra.adivinada";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDomainEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishRoundStarted(RoundStartedEvent event) {
        try {
            redisTemplate.convertAndSend(ROUND_STARTED_CHANNEL, objectMapper.writeValueAsString(RoundStartedPayload.from(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("round started event could not be serialized", exception);
        }
    }

    @Override
    public void publishRoundGuessed(RoundGuessedEvent event) {
        try {
            redisTemplate.convertAndSend(ROUND_GUESSED_CHANNEL, objectMapper.writeValueAsString(RoundGuessedPayload.from(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("round guessed event could not be serialized", exception);
        }
    }

    private record RoundStartedPayload(
            String roomCode,
            String roundId,
            String word,
            String startedAt,
            String endsAt,
            String occurredAt
    ) {

        static RoundStartedPayload from(RoundStartedEvent event) {
            return new RoundStartedPayload(
                    event.roomCode(),
                    event.roundId().toString(),
                    event.word(),
                    event.startedAt().toString(),
                    event.endsAt().toString(),
                    event.occurredAt().toString()
            );
        }
    }

    private record RoundGuessedPayload(
            String roomCode,
            String roundId,
            String playerId,
            int score,
            String occurredAt
    ) {

        static RoundGuessedPayload from(RoundGuessedEvent event) {
            return new RoundGuessedPayload(
                    event.roomCode(),
                    event.roundId().toString(),
                    event.playerId().toString(),
                    event.score(),
                    event.occurredAt().toString()
            );
        }
    }
}
