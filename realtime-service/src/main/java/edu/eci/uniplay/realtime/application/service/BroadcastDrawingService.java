package edu.eci.uniplay.realtime.application.service;

import java.time.Clock;
import java.util.Map;

import edu.eci.uniplay.realtime.application.dto.BroadcastDrawingCommand;
import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import edu.eci.uniplay.realtime.application.port.in.BroadcastDrawingUseCase;
import edu.eci.uniplay.realtime.application.port.out.DrawingMessageBroker;
import edu.eci.uniplay.realtime.domain.model.CanvasPoint;
import edu.eci.uniplay.realtime.domain.model.DrawingDelta;
import edu.eci.uniplay.realtime.domain.model.StrokeStyle;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastDrawingService implements BroadcastDrawingUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastDrawingService.class);

    private final DrawingMessageBroker drawingMessageBroker;
    private final Clock clock;

    public BroadcastDrawingService(DrawingMessageBroker drawingMessageBroker, Clock clock) {
        this.drawingMessageBroker = drawingMessageBroker;
        this.clock = clock;
    }

    @Override
    public DrawingDeltaMessage broadcast(BroadcastDrawingCommand command) {
        DrawingDelta delta = new DrawingDelta(
                command.roomCode(),
                command.playerId(),
                new CanvasPoint(command.fromX(), command.fromY()),
                new CanvasPoint(command.toX(), command.toY()),
                new StrokeStyle(command.color(), command.width()),
                clock.instant()
        );
        DrawingDeltaMessage message = DrawingDeltaMessage.from(delta);

        drawingMessageBroker.sendDrawingDelta(message);
        LOGGER.info(Markers.appendEntries(Map.of(
                "salaId", message.roomCode(),
                "evento", "trazo.enviado",
                "timestamp", message.occurredAt().toString()
        )), "drawing delta broadcast");

        return message;
    }
}
