package edu.eci.uniplay.game.infrastructure.redis;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisDomainEventPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void publishesRoundStartedEventToExpectedChannel() {
        RedisDomainEventPublisher publisher = new RedisDomainEventPublisher(redisTemplate, new ObjectMapper());

        publisher.publishRoundStarted(new RoundStartedEvent(
                "ABC123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Campus",
                Instant.parse("2026-07-07T12:00:00Z"),
                Instant.parse("2026-07-07T12:01:00Z"),
                Instant.parse("2026-07-07T12:00:00Z")
        ));

        verify(redisTemplate).convertAndSend(
                eq(RedisDomainEventPublisher.ROUND_STARTED_CHANNEL),
                contains("\"endsAt\":\"2026-07-07T12:01:00Z\"")
        );
    }

    @Test
    void publishesRoundGuessedEventToExpectedChannel() {
        RedisDomainEventPublisher publisher = new RedisDomainEventPublisher(redisTemplate, new ObjectMapper());

        publisher.publishRoundGuessed(new RoundGuessedEvent(
                "ABC123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                100,
                Instant.parse("2026-07-07T12:00:00Z")
        ));

        verify(redisTemplate).convertAndSend(
                eq(RedisDomainEventPublisher.ROUND_GUESSED_CHANNEL),
                contains("\"score\":100")
        );
    }

    @Test
    void publishesRoundFinishedEventToExpectedChannel() {
        RedisDomainEventPublisher publisher = new RedisDomainEventPublisher(redisTemplate, new ObjectMapper());

        publisher.publishRoundFinished(new RoundFinishedEvent(
                "ABC123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "EXPIRED",
                "TIMEOUT",
                Instant.parse("2026-07-07T12:01:00Z"),
                Instant.parse("2026-07-07T12:01:00Z")
        ));

        verify(redisTemplate).convertAndSend(
                eq(RedisDomainEventPublisher.ROUND_FINISHED_CHANNEL),
                contains("\"reason\":\"TIMEOUT\"")
        );
    }
}
