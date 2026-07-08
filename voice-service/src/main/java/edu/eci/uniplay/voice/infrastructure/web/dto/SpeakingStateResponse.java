package edu.eci.uniplay.voice.infrastructure.web.dto;

import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.SpeakingStateResult;

public record SpeakingStateResponse(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean speaking,
        Instant changedAt
) {
    public static SpeakingStateResponse from(SpeakingStateResult result) {
        return new SpeakingStateResponse(
                result.roomCode(),
                result.voiceRoomName(),
                result.participantIdentity(),
                result.speaking(),
                result.changedAt()
        );
    }
}
