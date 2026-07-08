package edu.eci.uniplay.voice.application.event;

import java.time.Instant;

public record SpeakingStateChangedEvent(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean speaking,
        Instant occurredAt
) {
}
