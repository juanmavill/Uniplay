package edu.eci.uniplay.voice.infrastructure.web.dto;

import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.MuteStateResult;

public record MuteStateResponse(
        String roomCode,
        String voiceRoomName,
        String participantIdentity,
        boolean muted,
        Instant changedAt
) {
    public static MuteStateResponse from(MuteStateResult result) {
        return new MuteStateResponse(
                result.roomCode(),
                result.voiceRoomName(),
                result.participantIdentity(),
                result.muted(),
                result.changedAt()
        );
    }
}
