package edu.eci.uniplay.realtime.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.realtime.application.dto.BroadcastDrawingCommand;
import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import edu.eci.uniplay.realtime.application.port.in.BroadcastDrawingUseCase;
import edu.eci.uniplay.realtime.infrastructure.websocket.dto.DrawingDeltaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrawingControllerTest {

    @Mock
    private BroadcastDrawingUseCase broadcastDrawingUseCase;

    @Test
    void delegatesDrawingPayloadToUseCase() {
        DrawingController controller = new DrawingController(broadcastDrawingUseCase);
        UUID playerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        DrawingDeltaMessage expectedMessage = new DrawingDeltaMessage(
                "ABC123",
                playerId,
                0.1,
                0.2,
                0.3,
                0.4,
                "#00FFAA",
                4,
                Instant.parse("2026-07-07T12:00:00Z")
        );
        when(broadcastDrawingUseCase.broadcast(any(BroadcastDrawingCommand.class))).thenReturn(expectedMessage);

        DrawingDeltaMessage response = controller.draw(
                "ABC123",
                new DrawingDeltaRequest(playerId, 0.1, 0.2, 0.3, 0.4, "#00FFAA", 4)
        );

        assertThat(response).isEqualTo(expectedMessage);

        ArgumentCaptor<BroadcastDrawingCommand> commandCaptor = ArgumentCaptor.forClass(BroadcastDrawingCommand.class);
        verify(broadcastDrawingUseCase).broadcast(commandCaptor.capture());
        assertThat(commandCaptor.getValue().roomCode()).isEqualTo("ABC123");
        assertThat(commandCaptor.getValue().playerId()).isEqualTo(playerId);
    }
}
