package edu.eci.uniplay.realtime.infrastructure.websocket;

import edu.eci.uniplay.realtime.application.dto.BroadcastDrawingCommand;
import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import edu.eci.uniplay.realtime.application.port.in.BroadcastDrawingUseCase;
import edu.eci.uniplay.realtime.infrastructure.websocket.dto.DrawingDeltaRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class DrawingController {

    private final BroadcastDrawingUseCase broadcastDrawingUseCase;

    public DrawingController(BroadcastDrawingUseCase broadcastDrawingUseCase) {
        this.broadcastDrawingUseCase = broadcastDrawingUseCase;
    }

    @MessageMapping("/rooms/{roomCode}/draw")
    public DrawingDeltaMessage draw(
            @DestinationVariable String roomCode,
            @Valid DrawingDeltaRequest request
    ) {
        return broadcastDrawingUseCase.broadcast(new BroadcastDrawingCommand(
                roomCode,
                request.playerId(),
                request.fromX(),
                request.fromY(),
                request.toX(),
                request.toY(),
                request.color(),
                request.width()
        ));
    }
}
