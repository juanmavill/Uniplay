package edu.eci.uniplay.realtime.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import edu.eci.uniplay.realtime.application.dto.BroadcastDrawingCommand;
import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import edu.eci.uniplay.realtime.application.port.out.DrawingMessageBroker;
import edu.eci.uniplay.realtime.domain.model.InvalidDrawingDeltaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BroadcastDrawingServiceTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-07T12:00:00Z");

    @Mock
    private DrawingMessageBroker drawingMessageBroker;

    private BroadcastDrawingService broadcastDrawingService;

    @BeforeEach
    void setUp() {
        broadcastDrawingService = new BroadcastDrawingService(
                drawingMessageBroker,
                Clock.fixed(OCCURRED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void broadcastsDrawingDelta() {
        UUID playerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        DrawingDeltaMessage message = broadcastDrawingService.broadcast(new BroadcastDrawingCommand(
                "abc123",
                playerId,
                0.1,
                0.2,
                0.3,
                0.4,
                "#00ffaa",
                4
        ));

        assertThat(message.roomCode()).isEqualTo("ABC123");
        assertThat(message.playerId()).isEqualTo(playerId);
        assertThat(message.color()).isEqualTo("#00FFAA");
        assertThat(message.occurredAt()).isEqualTo(OCCURRED_AT);

        ArgumentCaptor<DrawingDeltaMessage> messageCaptor = ArgumentCaptor.forClass(DrawingDeltaMessage.class);
        verify(drawingMessageBroker).sendDrawingDelta(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo(message);
    }

    @Test
    void doesNotBroadcastInvalidDelta() {
        BroadcastDrawingCommand command = new BroadcastDrawingCommand(
                "ABC123",
                UUID.randomUUID(),
                1.2,
                0.2,
                0.3,
                0.4,
                "#00FFAA",
                4
        );

        assertThatThrownBy(() -> broadcastDrawingService.broadcast(command))
                .isInstanceOf(InvalidDrawingDeltaException.class)
                .hasMessage("canvas coordinates must be normalized between 0 and 1");

        verify(drawingMessageBroker, never()).sendDrawingDelta(org.mockito.ArgumentMatchers.any());
    }
}
