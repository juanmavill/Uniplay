package edu.eci.uniplay.voice.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import edu.eci.uniplay.voice.application.event.SpeakingStateChangedEvent;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static net.logstash.logback.marker.Markers.append;

public class RedisVoiceEventPublisher implements VoiceEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisVoiceEventPublisher.class);

    static final String MUTE_STATE_CHANGED_CHANNEL = "voz.microfono_actualizado";
    static final String SPEAKING_STATE_CHANGED_CHANNEL = "voz.jugador_hablando";

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
            logPublished(MUTE_STATE_CHANGED_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("mute state event could not be serialized", exception);
        }
    }

    @Override
    public void publishSpeakingStateChanged(SpeakingStateChangedEvent event) {
        try {
            redisTemplate.convertAndSend(
                    SPEAKING_STATE_CHANGED_CHANNEL,
                    objectMapper.writeValueAsString(SpeakingStateChangedPayload.from(event))
            );
            logPublished(SPEAKING_STATE_CHANGED_CHANNEL, event.roomCode(), event.occurredAt().toString());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("speaking state event could not be serialized", exception);
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

    private record SpeakingStateChangedPayload(
            String roomCode,
            String voiceRoomName,
            String participantIdentity,
            boolean speaking,
            String occurredAt
    ) {
        static SpeakingStateChangedPayload from(SpeakingStateChangedEvent event) {
            return new SpeakingStateChangedPayload(
                    event.roomCode(),
                    event.voiceRoomName(),
                    event.participantIdentity(),
                    event.speaking(),
                    event.occurredAt().toString()
            );
        }
    }
}
