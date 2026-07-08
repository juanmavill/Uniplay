package edu.eci.uniplay.realtime.infrastructure.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.realtime.application.dto.VoiceEventMessage;
import edu.eci.uniplay.realtime.application.port.out.VoiceEventBroker;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisVoiceEventSubscriber implements MessageListener {

    public static final String SPEAKING_STATE_CHANGED_CHANNEL = "voz.jugador_hablando";

    private final ObjectMapper objectMapper;
    private final VoiceEventBroker voiceEventBroker;

    public RedisVoiceEventSubscriber(ObjectMapper objectMapper, VoiceEventBroker voiceEventBroker) {
        this.objectMapper = objectMapper;
        this.voiceEventBroker = voiceEventBroker;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            voiceEventBroker.sendVoiceEvent(toMessage(channel, payload));
        } catch (IOException exception) {
            throw new IllegalStateException("voice event could not be deserialized", exception);
        }
    }

    private VoiceEventMessage toMessage(String channel, String payload) throws IOException {
        return switch (channel) {
            case SPEAKING_STATE_CHANGED_CHANNEL -> toSpeakingStateChangedMessage(payload);
            default -> throw new IllegalArgumentException("unsupported voice event channel " + channel);
        };
    }

    private VoiceEventMessage toSpeakingStateChangedMessage(String payload) throws IOException {
        SpeakingStateChangedPayload event = objectMapper.readValue(payload, SpeakingStateChangedPayload.class);
        return new VoiceEventMessage(
                "VOICE_SPEAKING_CHANGED",
                event.roomCode(),
                event.voiceRoomName(),
                event.participantIdentity(),
                event.speaking(),
                Instant.parse(event.occurredAt())
        );
    }

    private record SpeakingStateChangedPayload(
            String roomCode,
            String voiceRoomName,
            String participantIdentity,
            boolean speaking,
            String occurredAt
    ) {
    }
}
