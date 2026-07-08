package edu.eci.uniplay.voice.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisVoiceEventPublisher implements VoiceEventPublisher {

    static final String MUTE_STATE_CHANGED_CHANNEL = "voz.microfono_actualizado";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisVoiceEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishMuteStateChanged(MuteStateChangedEvent event) {
        try {
            redisTemplate.convertAndSend(
                    MUTE_STATE_CHANGED_CHANNEL,
                    objectMapper.writeValueAsString(MuteStateChangedPayload.from(event))
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("mute state event could not be serialized", exception);
        }
    }

    private record MuteStateChangedPayload(
            String roomCode,
            String voiceRoomName,
            String participantIdentity,
            boolean muted,
            String occurredAt
    ) {
        static MuteStateChangedPayload from(MuteStateChangedEvent event) {
            return new MuteStateChangedPayload(
                    event.roomCode(),
                    event.voiceRoomName(),
                    event.participantIdentity(),
                    event.muted(),
                    event.occurredAt().toString()
            );
        }
    }
}
