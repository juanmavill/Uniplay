package edu.eci.uniplay.voice.application.event;

import java.time.Instant;

public record MuteStateChangedEvent(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean muted,
        Instant occurredAt
) {
}
