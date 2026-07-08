package edu.eci.uniplay.voice.infrastructure.livekit;

import java.time.Duration;

import edu.eci.uniplay.voice.application.port.out.VoiceTokenIssuer;
import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.ParticipantName;
import edu.eci.uniplay.voice.domain.model.RoomCode;
import edu.eci.uniplay.voice.infrastructure.config.VoiceProperties;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;

public class LiveKitVoiceTokenIssuer implements VoiceTokenIssuer {

    private final VoiceProperties voiceProperties;

    public LiveKitVoiceTokenIssuer(VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
    }

    @Override
    public String issueJoinToken(
            RoomCode roomCode,
            ParticipantIdentity participantIdentity,
            ParticipantName participantName,
            Duration ttl
    ) {
        AccessToken token = new AccessToken(voiceProperties.apiKey(), voiceProperties.apiSecret());
        token.setIdentity(participantIdentity.asString());
        token.setName(participantName.value());
        token.addGrants(new RoomJoin(true), new RoomName(roomCode.voiceRoomName()));
        token.setTtl(ttl.toMillis());
        return token.toJwt();
    }
}
