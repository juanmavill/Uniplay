package edu.eci.uniplay.room.infrastructure.redis;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.event.PlayerJoinedEvent;
import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisDomainEventPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void publishesRoomCreatedEventToExpectedChannel() {
        RedisDomainEventPublisher publisher = new RedisDomainEventPublisher(redisTemplate, new ObjectMapper());
        RoomCreatedEvent event = new RoomCreatedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ABC123",
                21,
                Instant.parse("2026-07-07T12:00:00Z")
        );

        publisher.publishRoomCreated(event);

        verify(redisTemplate).convertAndSend(
                eq(RedisDomainEventPublisher.ROOM_CREATED_CHANNEL),
                contains("\"code\":\"ABC123\"")
        );
    }

    @Test
    void publishesPlayerJoinedEventToExpectedChannel() {
        RedisDomainEventPublisher publisher = new RedisDomainEventPublisher(redisTemplate, new ObjectMapper());
        PlayerJoinedEvent event = new PlayerJoinedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ABC123",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Ana",
                Instant.parse("2026-07-07T12:00:00Z")
        );

        publisher.publishPlayerJoined(event);

        verify(redisTemplate).convertAndSend(
                eq(RedisDomainEventPublisher.PLAYER_JOINED_CHANNEL),
                contains("\"playerName\":\"Ana\"")
        );
    }
}
