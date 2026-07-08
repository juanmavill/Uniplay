package edu.eci.uniplay.realtime.infrastructure.stomp;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class SpringStompDrawingMessageBrokerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsDrawingDeltaToRoomTopic() {
        SpringStompDrawingMessageBroker broker = new SpringStompDrawingMessageBroker(messagingTemplate);
        DrawingDeltaMessage message = new DrawingDeltaMessage(
                "ABC123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                0.1,
                0.2,
                0.3,
                0.4,
                "#00FFAA",
                4,
                Instant.parse("2026-07-07T12:00:00Z")
        );

        broker.sendDrawingDelta(message);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC123/draw", message);
    }
}
