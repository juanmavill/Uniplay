package edu.eci.uniplay.room.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import edu.eci.uniplay.room.domain.model.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRoomRepositoryTest {

    private static final Duration TTL = Duration.ofHours(2);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRoomRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new RedisRoomRepository(redisTemplate, new ObjectMapper(), TTL);
    }

    @Test
    void storesRoomWhenCodeCanBeReserved() {
        Room room = Room.create(RoomId.newId(), new RoomCode("ABC123"), 21, Instant.parse("2026-07-07T12:00:00Z"));
        when(valueOperations.setIfAbsent("room-code:ABC123", room.id().value().toString(), TTL)).thenReturn(true);

        boolean saved = repository.saveIfCodeAvailable(room);

        assertThat(saved).isTrue();
        verify(valueOperations).set(eq("room:" + room.id().value()), any(String.class), eq(TTL));
    }

    @Test
    void doesNotStoreRoomWhenCodeIsAlreadyReserved() {
        Room room = Room.create(RoomId.newId(), new RoomCode("ABC123"), 21, Instant.parse("2026-07-07T12:00:00Z"));
        when(valueOperations.setIfAbsent("room-code:ABC123", room.id().value().toString(), TTL)).thenReturn(false);

        boolean saved = repository.saveIfCodeAvailable(room);

        assertThat(saved).isFalse();
        verify(valueOperations, never()).set(eq("room:" + room.id().value()), any(String.class), eq(TTL));
    }
}
