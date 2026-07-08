package edu.eci.uniplay.room.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.event.PlayerJoinedEvent;
import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static net.logstash.logback.marker.Markers.append;

public class RedisDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDomainEventPublisher.class);

    static final String ROOM_CREATED_CHANNEL = "sala.creada";
    static final String PLAYER_JOINED_CHANNEL = "jugador.conectado";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDomainEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishRoomCreated(RoomCreatedEvent event) {
        try {
            redisTemplate.convertAndSend(ROOM_CREATED_CHANNEL, objectMapper.writeValueAsString(RoomCreatedPayload.from(event)));
            logPublished(ROOM_CREATED_CHANNEL, event.code(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("room created event could not be serialized", exception);
        }
    }

    @Override
    public void publishPlayerJoined(PlayerJoinedEvent event) {
        try {
            redisTemplate.convertAndSend(PLAYER_JOINED_CHANNEL, objectMapper.writeValueAsString(PlayerJoinedPayload.from(event)));
            logPublished(PLAYER_JOINED_CHANNEL, event.code(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("player joined event could not be serialized", exception);
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

    private record RoomCreatedPayload(String roomId, String code, int maxPlayers, String occurredAt) {

        static RoomCreatedPayload from(RoomCreatedEvent event) {
            return new RoomCreatedPayload(
                    event.roomId().toString(),
                    event.code(),
                    event.maxPlayers(),
                    event.occurredAt().toString()
            );
        }
    }

    private record PlayerJoinedPayload(
            String roomId,
            String code,
            String playerId,
            String playerName,
            String occurredAt
    ) {

        static PlayerJoinedPayload from(PlayerJoinedEvent event) {
            return new PlayerJoinedPayload(
                    event.roomId().toString(),
                    event.code(),
                    event.playerId().toString(),
                    event.playerName(),
                    event.occurredAt().toString()
            );
        }
    }
}
