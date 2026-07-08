package edu.eci.uniplay.voice.application.port.out;

import java.time.Duration;

import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.ParticipantName;
import edu.eci.uniplay.voice.domain.model.RoomCode;

public interface VoiceTokenIssuer {

    String issueJoinToken(
            RoomCode roomCode,
            ParticipantIdentity participantIdentity,
            ParticipantName participantName,
            Duration ttl
    );
}
