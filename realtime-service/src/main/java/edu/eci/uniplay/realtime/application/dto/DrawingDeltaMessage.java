package edu.eci.uniplay.realtime.application.dto;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.realtime.domain.model.DrawingDelta;

public record DrawingDeltaMessage(
        String roomCode,
        UUID playerId,
        double fromX,
        double fromY,
        double toX,
        double toY,
        String color,
        double width,
        Instant occurredAt
) {

    public static DrawingDeltaMessage from(DrawingDelta delta) {
        return new DrawingDeltaMessage(
                delta.roomCode(),
                delta.playerId(),
                delta.from().x(),
                delta.from().y(),
                delta.to().x(),
                delta.to().y(),
                delta.strokeStyle().color(),
                delta.strokeStyle().width(),
                delta.occurredAt()
        );
    }
}
