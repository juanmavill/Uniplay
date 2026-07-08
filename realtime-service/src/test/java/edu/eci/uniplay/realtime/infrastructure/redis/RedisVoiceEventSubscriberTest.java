package edu.eci.uniplay.realtime.infrastructure.redis;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.realtime.application.dto.VoiceEventMessage;
import edu.eci.uniplay.realtime.application.port.out.VoiceEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisVoiceEventSubscriberTest {

    @Mock
    private VoiceEventBroker voiceEventBroker;

    @Test
    void forwardsSpeakingStateChangedEvent() {
        RedisVoiceEventSubscriber subscriber = new RedisVoiceEventSubscriber(new ObjectMapper(), voiceEventBroker);

        subscriber.onMessage(message(RedisVoiceEventSubscriber.SPEAKING_STATE_CHANGED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "voiceRoomName": "uniplay-ABC123",
                  "participantIdentity": "22222222-2222-2222-2222-222222222222",
                  "speaking": true,
                  "occurredAt": "2026-07-07T12:00:00Z"
                }
                """), null);

        ArgumentCaptor<VoiceEventMessage> captor = ArgumentCaptor.forClass(VoiceEventMessage.class);
        verify(voiceEventBroker).sendVoiceEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("VOICE_SPEAKING_CHANGED");
        assertThat(captor.getValue().speaking()).isTrue();
    }

    private DefaultMessage message(String channel, String body) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }
}
