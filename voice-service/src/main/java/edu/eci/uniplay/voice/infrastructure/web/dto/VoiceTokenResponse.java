package edu.eci.uniplay.voice.infrastructure.web.dto;

import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.VoiceTokenResult;

public record VoiceTokenResponse(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        String participantName,
        String livekitUrl,
        String token,
        Instant expiresAt
) {
    public static VoiceTokenResponse from(VoiceTokenResult result) {
        return new VoiceTokenResponse(
                result.roomCode(),
                result.voiceRoomName(),
                result.participantIdentity(),
                result.participantName(),
                result.livekitUrl(),
                result.token(),
                result.expiresAt()
        );
    }
}
