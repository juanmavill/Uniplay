package edu.eci.uniplay.realtime.application.dto;

import java.util.UUID;

public record BroadcastDrawingCommand(
        String roomCode,
        UUID playerId,
        double fromX,
        double fromY,
        double toX,
        double toY,
        String color,
        double width
) {
}
