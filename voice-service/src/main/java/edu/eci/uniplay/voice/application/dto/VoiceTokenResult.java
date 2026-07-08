package edu.eci.uniplay.voice.application.dto;

import java.time.Instant;

public record VoiceTokenResult(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        String participantName,
        String livekitUrl,
        String token,
        Instant expiresAt
) {
}
