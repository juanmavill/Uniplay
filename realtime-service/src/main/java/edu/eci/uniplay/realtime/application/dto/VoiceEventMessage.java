package edu.eci.uniplay.realtime.application.dto;

import java.time.Instant;

public record VoiceEventMessage(
        String type,
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        Boolean speaking,
        Instant occurredAt
) {
}
