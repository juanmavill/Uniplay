package edu.eci.uniplay.room.infrastructure.redis;

import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Room;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisRoomRepository implements RoomRepository {

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String ROOM_CODE_KEY_PREFIX = "room-code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisRoomRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public boolean saveIfCodeAvailable(Room room) {
        String codeKey = codeKey(room);
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(codeKey, room.id().value().toString(), ttl);

        if (!Boolean.TRUE.equals(reserved)) {
            return false;
        }

        try {
            redisTemplate.opsForValue().set(roomKey(room), serialize(room), ttl);
            return true;
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(codeKey);
            throw new IllegalStateException("room could not be serialized", exception);
        }
    }

    private String serialize(Room room) throws JsonProcessingException {
        return objectMapper.writeValueAsString(RoomDocument.from(room));
    }

    private String roomKey(Room room) {
        return ROOM_KEY_PREFIX + room.id().value();
    }

    private String codeKey(Room room) {
        return ROOM_CODE_KEY_PREFIX + room.code().value();
    }

    private record RoomDocument(
            String id,
            String code,
            String status,
            int maxPlayers,
            String createdAt
    ) {

        static RoomDocument from(Room room) {
            return new RoomDocument(
                    room.id().value().toString(),
                    room.code().value(),
                    room.status().name(),
                    room.maxPlayers(),
                    room.createdAt().toString()
            );
        }
    }
}
