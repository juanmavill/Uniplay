package edu.eci.uniplay.game.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.event.VoteCastEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static net.logstash.logback.marker.Markers.append;

public class RedisDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDomainEventPublisher.class);

    static final String ROUND_STARTED_CHANNEL = "ronda.iniciada";
    static final String ROUND_GUESSED_CHANNEL = "palabra.adivinada";
    static final String ROUND_FINISHED_CHANNEL = "ronda.terminada";
    static final String VOTE_CAST_CHANNEL = "voto.emitido";

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
            logPublished(ROUND_STARTED_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("round started event could not be serialized", exception);
        }
    }

    @Override
    public void publishRoundGuessed(RoundGuessedEvent event) {
        try {
            redisTemplate.convertAndSend(ROUND_GUESSED_CHANNEL, objectMapper.writeValueAsString(RoundGuessedPayload.from(event)));
            logPublished(ROUND_GUESSED_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("round guessed event could not be serialized", exception);
        }
    }

    @Override
    public void publishRoundFinished(RoundFinishedEvent event) {
        try {
            redisTemplate.convertAndSend(ROUND_FINISHED_CHANNEL, objectMapper.writeValueAsString(RoundFinishedPayload.from(event)));
            logPublished(ROUND_FINISHED_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("round finished event could not be serialized", exception);
        }
    }

    @Override
    public void publishVoteCast(VoteCastEvent event) {
        try {
            redisTemplate.convertAndSend(VOTE_CAST_CHANNEL, objectMapper.writeValueAsString(VoteCastPayload.from(event)));
            logPublished(VOTE_CAST_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("vote cast event could not be serialized", exception);
        }
    }

    private void logPublished(String eventName, String roomCode, String timestamp) {
        LOGGER.info(
                append("evento", eventName)
                        .and(append("salaId", roomCode))
                        .and(append("timestamp", timestamp)),
                "domain event published"
        );
    }

    private record RoundStartedPayload(
            String roomCode,
            String roundId,
            String mode,
            String deck,
            String startedAt,
            String endsAt,
            String occurredAt
    ) {

        static RoundStartedPayload from(RoundStartedEvent event) {
            return new RoundStartedPayload(
                    event.roomCode(),
                    event.roundId().toString(),
                    event.mode(),
                    event.deck(),
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

    private record RoundFinishedPayload(
            String roomCode,
            String roundId,
            String status,
            String reason,
            String finishedAt,
            String occurredAt
    ) {

        static RoundFinishedPayload from(RoundFinishedEvent event) {
            return new RoundFinishedPayload(
                    event.roomCode(),
                    event.roundId().toString(),
                    event.status(),
                    event.reason(),
                    event.finishedAt().toString(),
                    event.occurredAt().toString()
            );
        }
    }

    private record VoteCastPayload(
            String roomCode,
            String roundId,
            String voterId,
            String candidateId,
            java.util.List<VoteTallyPayload> tallies,
            String occurredAt
    ) {

        static VoteCastPayload from(VoteCastEvent event) {
            return new VoteCastPayload(
                    event.roomCode(),
                    event.roundId().toString(),
                    event.voterId().toString(),
                    event.candidateId().toString(),
                    event.tallies().stream()
                            .map(tally -> new VoteTallyPayload(tally.candidateId().toString(), tally.votes()))
                            .toList(),
                    event.occurredAt().toString()
            );
        }
    }

    private record VoteTallyPayload(String candidateId, int votes) {
    }
}
