package edu.eci.uniplay.realtime.infrastructure.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.realtime.application.dto.RoundEventMessage;
import edu.eci.uniplay.realtime.application.port.out.RoundEventBroker;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisRoundEventSubscriber implements MessageListener {

    public static final String ROUND_STARTED_CHANNEL = "ronda.iniciada";
    public static final String ROUND_FINISHED_CHANNEL = "ronda.terminada";
    public static final String ROUND_GUESSED_CHANNEL = "palabra.adivinada";

    private final ObjectMapper objectMapper;
    private final RoundEventBroker roundEventBroker;

    public RedisRoundEventSubscriber(ObjectMapper objectMapper, RoundEventBroker roundEventBroker) {
        this.objectMapper = objectMapper;
        this.roundEventBroker = roundEventBroker;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            roundEventBroker.sendRoundEvent(toMessage(channel, payload));
        } catch (IOException exception) {
            throw new IllegalStateException("round event could not be deserialized", exception);
        }
    }

    private RoundEventMessage toMessage(String channel, String payload) throws IOException {
        return switch (channel) {
            case ROUND_STARTED_CHANNEL -> toRoundStartedMessage(payload);
            case ROUND_FINISHED_CHANNEL -> toRoundFinishedMessage(payload);
            case ROUND_GUESSED_CHANNEL -> toRoundGuessedMessage(payload);
            default -> throw new IllegalArgumentException("unsupported round event channel " + channel);
        };
    }

    private RoundEventMessage toRoundStartedMessage(String payload) throws IOException {
        RoundStartedPayload event = objectMapper.readValue(payload, RoundStartedPayload.class);
        return new RoundEventMessage(
                "ROUND_STARTED",
                event.roomCode(),
                UUID.fromString(event.roundId()),
                event.word(),
                "ACTIVE",
                null,
                null,
                null,
                Instant.parse(event.startedAt()),
                Instant.parse(event.endsAt()),
                null,
                Instant.parse(event.occurredAt())
        );
    }

    private RoundEventMessage toRoundFinishedMessage(String payload) throws IOException {
        RoundFinishedPayload event = objectMapper.readValue(payload, RoundFinishedPayload.class);
        return new RoundEventMessage(
                "ROUND_FINISHED",
                event.roomCode(),
                UUID.fromString(event.roundId()),
                null,
                event.status(),
                event.reason(),
                null,
                null,
                null,
                null,
                Instant.parse(event.finishedAt()),
                Instant.parse(event.occurredAt())
        );
    }

    private RoundEventMessage toRoundGuessedMessage(String payload) throws IOException {
        RoundGuessedPayload event = objectMapper.readValue(payload, RoundGuessedPayload.class);
        return new RoundEventMessage(
                "WORD_GUESSED",
                event.roomCode(),
                UUID.fromString(event.roundId()),
                null,
                null,
                null,
                UUID.fromString(event.playerId()),
                event.score(),
                null,
                null,
                null,
                Instant.parse(event.occurredAt())
        );
    }

    private record RoundStartedPayload(
            String roomCode,
            String roundId,
            String word,
            String startedAt,
            String endsAt,
            String occurredAt
    ) {
    }

    private record RoundFinishedPayload(
            String roomCode,
            String roundId,
            String status,
            String reason,
            String finishedAt,
            String occurredAt
    ) {
    }

    private record RoundGuessedPayload(
            String roomCode,
            String roundId,
            String playerId,
            int score,
            String occurredAt
    ) {
    }
}
