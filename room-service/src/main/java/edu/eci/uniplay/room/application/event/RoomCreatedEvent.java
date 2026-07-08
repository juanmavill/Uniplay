package edu.eci.uniplay.room.application.event;

import java.time.Instant;
import java.util.UUID;

public record RoomCreatedEvent(UUID roomId, String code, int maxPlayers, Instant occurredAt) {
}
