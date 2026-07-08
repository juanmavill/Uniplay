package edu.eci.uniplay.room.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisDomainEventPublisher implements DomainEventPublisher {

    static final String ROOM_CREATED_CHANNEL = "sala.creada";

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
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("room created event could not be serialized", exception);
        }
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
}
