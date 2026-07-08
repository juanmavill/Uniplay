package edu.eci.uniplay.realtime.infrastructure.stomp;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.realtime.application.dto.RoundEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringStompRoundEventBrokerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendsRoundEventToRoomTopic() {
        SpringStompRoundEventBroker broker = new SpringStompRoundEventBroker(messagingTemplate);
        RoundEventMessage message = new RoundEventMessage(
                "ROUND_STARTED",
                "ABC123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Campus",
                "ACTIVE",
                null,
                null,
                null,
                Instant.parse("2026-07-07T12:00:00Z"),
                Instant.parse("2026-07-07T12:01:00Z"),
                null,
                Instant.parse("2026-07-07T12:00:00Z")
        );

        broker.sendRoundEvent(message);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC123/rounds", message);
    }
}
