package edu.eci.uniplay.voice.infrastructure.redis;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisVoiceEventPublisherTest {

    @Test
    void publishesMuteStateChangedEvent() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisVoiceEventPublisher publisher = new RedisVoiceEventPublisher(redisTemplate, new ObjectMapper());

        publisher.publishMuteStateChanged(new MuteStateChangedEvent(
                "ABC123",
                "uniplay-ABC123",
                "22222222-2222-2222-2222-222222222222",
                true,
                Instant.parse("2026-07-07T12:00:00Z")
        ));

        verify(redisTemplate).convertAndSend(
                eq("voz.microfono_actualizado"),
                contains("\"muted\":true")
        );
    }
}
