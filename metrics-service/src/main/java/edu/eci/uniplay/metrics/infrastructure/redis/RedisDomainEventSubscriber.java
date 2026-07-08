package edu.eci.uniplay.metrics.infrastructure.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.metrics.application.port.in.RecordDomainEventUseCase;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisDomainEventSubscriber implements MessageListener {

    public static final String ROOM_CREATED_CHANNEL = "sala.creada";
    public static final String PLAYER_CONNECTED_CHANNEL = "jugador.conectado";
    public static final String ROUND_STARTED_CHANNEL = "ronda.iniciada";
    public static final String WORD_GUESSED_CHANNEL = "palabra.adivinada";

    private final ObjectMapper objectMapper;
    private final RecordDomainEventUseCase recordDomainEventUseCase;

    public RedisDomainEventSubscriber(ObjectMapper objectMapper, RecordDomainEventUseCase recordDomainEventUseCase) {
        this.objectMapper = objectMapper;
        this.recordDomainEventUseCase = recordDomainEventUseCase;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            record(channel, objectMapper.readTree(payload));
        } catch (IOException exception) {
            throw new IllegalStateException("metrics event could not be deserialized", exception);
        }
    }

    private void record(String channel, JsonNode payload) {
        switch (channel) {
            case ROOM_CREATED_CHANNEL -> recordDomainEventUseCase.recordRoomCreated(requiredText(payload, "code"));
            case PLAYER_CONNECTED_CHANNEL -> recordDomainEventUseCase.recordPlayerConnected(
                    requiredText(payload, "code"),
                    requiredText(payload, "playerId")
            );
            case ROUND_STARTED_CHANNEL -> recordDomainEventUseCase.recordRoundStarted(
                    requiredText(payload, "roomCode"),
                    requiredText(payload, "roundId")
            );
            case WORD_GUESSED_CHANNEL -> recordDomainEventUseCase.recordWordGuessed(
                    requiredText(payload, "roomCode"),
                    requiredText(payload, "roundId")
            );
            default -> throw new IllegalArgumentException("unsupported metrics event channel " + channel);
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException("event field " + field + " is required");
        }
        return value.asText();
    }
}
