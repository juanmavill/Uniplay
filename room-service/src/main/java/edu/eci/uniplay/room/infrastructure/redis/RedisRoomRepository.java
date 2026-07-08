package edu.eci.uniplay.room.infrastructure.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Player;
import edu.eci.uniplay.room.domain.model.PlayerId;
import edu.eci.uniplay.room.domain.model.PlayerName;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import edu.eci.uniplay.room.domain.model.RoomId;
import edu.eci.uniplay.room.domain.model.RoomStatus;
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

    @Override
    public Optional<Room> findByCode(RoomCode code) {
        String roomId = redisTemplate.opsForValue().get(codeKey(code));

        if (roomId == null) {
            return Optional.empty();
        }

        String roomJson = redisTemplate.opsForValue().get(roomKey(roomId));

        if (roomJson == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(roomJson, RoomDocument.class).toDomain());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("room could not be deserialized", exception);
        }
    }

    @Override
    public void save(Room room) {
        try {
            redisTemplate.opsForValue().set(roomKey(room), serialize(room), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("room could not be serialized", exception);
        }
    }

    private String serialize(Room room) throws JsonProcessingException {
        return objectMapper.writeValueAsString(RoomDocument.from(room));
    }

    private String roomKey(Room room) {
        return roomKey(room.id().value().toString());
    }

    private String roomKey(String roomId) {
        return ROOM_KEY_PREFIX + roomId;
    }

    private String codeKey(Room room) {
        return codeKey(room.code());
    }

    private String codeKey(RoomCode code) {
        return ROOM_CODE_KEY_PREFIX + code.value();
    }

    private record RoomDocument(
            String id,
            String code,
            String status,
            int maxPlayers,
            String createdAt,
            List<PlayerDocument> players
    ) {

        static RoomDocument from(Room room) {
            return new RoomDocument(
                    room.id().value().toString(),
                    room.code().value(),
                    room.status().name(),
                    room.maxPlayers(),
                    room.createdAt().toString(),
                    room.players().stream().map(PlayerDocument::from).toList()
            );
        }

        Room toDomain() {
            return Room.restore(
                    new RoomId(UUID.fromString(id)),
                    new RoomCode(code),
                    RoomStatus.valueOf(status),
                    maxPlayers,
                    Instant.parse(createdAt),
                    players.stream().map(PlayerDocument::toDomain).toList()
            );
        }
    }

    private record PlayerDocument(String id, String name) {

        static PlayerDocument from(Player player) {
            return new PlayerDocument(player.id().value().toString(), player.name().value());
        }

        Player toDomain() {
            return new Player(new PlayerId(UUID.fromString(id)), new PlayerName(name));
        }
    }
}
