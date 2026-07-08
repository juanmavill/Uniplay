package edu.eci.uniplay.voice.application.dto;

import java.time.Instant;

public record SpeakingStateResult(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean speaking,
        Instant changedAt
) {
}
