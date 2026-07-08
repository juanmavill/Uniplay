package edu.eci.uniplay.voice.application.service;

import java.time.Clock;
import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.GenerateVoiceTokenCommand;
import edu.eci.uniplay.voice.application.dto.VoiceTokenResult;
import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
import edu.eci.uniplay.voice.application.port.out.VoiceTokenIssuer;
import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.ParticipantName;
import edu.eci.uniplay.voice.domain.model.RoomCode;
import edu.eci.uniplay.voice.infrastructure.config.VoiceProperties;

public class GenerateVoiceTokenService implements GenerateVoiceTokenUseCase {

    private final VoiceTokenIssuer voiceTokenIssuer;
    private final VoiceProperties voiceProperties;
    private final Clock clock;

    public GenerateVoiceTokenService(
            VoiceTokenIssuer voiceTokenIssuer,
            VoiceProperties voiceProperties,
            Clock clock
    ) {
        this.voiceTokenIssuer = voiceTokenIssuer;
        this.voiceProperties = voiceProperties;
        this.clock = clock;
    }

    @Override
    public VoiceTokenResult generateToken(GenerateVoiceTokenCommand command) {
        RoomCode roomCode = RoomCode.from(command.roomCode());
        ParticipantIdentity participantIdentity = ParticipantIdentity.from(command.playerId());
        ParticipantName participantName = new ParticipantName(command.playerName());
        String token = voiceTokenIssuer.issueJoinToken(
                roomCode,
                participantIdentity,
                participantName,
                voiceProperties.tokenTtl()
        );
        Instant expiresAt = clock.instant().plus(voiceProperties.tokenTtl());
        return new VoiceTokenResult(
                roomCode.value(),
                roomCode.voiceRoomName(),
                participantIdentity.asString(),
                participantName.value(),
                voiceProperties.publicUrl(),
                token,
                expiresAt
        );
    }
}
