package edu.eci.uniplay.voice.application.dto;

import java.time.Instant;

public record MuteStateResult(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean muted,
        Instant changedAt
) {
}
