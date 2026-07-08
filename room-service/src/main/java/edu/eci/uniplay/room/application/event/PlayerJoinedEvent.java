package edu.eci.uniplay.room.application.event;

import java.time.Instant;
import java.util.UUID;

public record PlayerJoinedEvent(UUID roomId, String code, UUID playerId, String playerName, Instant occurredAt) {
}
