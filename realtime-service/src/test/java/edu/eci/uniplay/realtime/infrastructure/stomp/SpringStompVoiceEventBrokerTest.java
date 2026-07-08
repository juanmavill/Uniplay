package edu.eci.uniplay.realtime.infrastructure.stomp;

import java.time.Instant;

import edu.eci.uniplay.realtime.application.dto.VoiceEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringStompVoiceEventBrokerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsVoiceEventToRoomTopic() {
        SpringStompVoiceEventBroker broker = new SpringStompVoiceEventBroker(messagingTemplate);
        VoiceEventMessage message = new VoiceEventMessage(
                "VOICE_SPEAKING_CHANGED",
                "ABC123",
                "uniplay-ABC123",
                "22222222-2222-2222-2222-222222222222",
                true,
                Instant.parse("2026-07-07T12:00:00Z")
        );

        broker.sendVoiceEvent(message);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC123/voice", message);
    }
}
