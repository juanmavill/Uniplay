package edu.eci.uniplay.voice.infrastructure.livekit;

import java.time.Duration;

import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.ParticipantName;
import edu.eci.uniplay.voice.domain.model.RoomCode;
import edu.eci.uniplay.voice.infrastructure.config.VoiceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveKitVoiceTokenIssuerTest {

    @Test
    void issuesSignedJoinToken() {
        LiveKitVoiceTokenIssuer issuer = new LiveKitVoiceTokenIssuer(new VoiceProperties(
                "ws://livekit:7880",
                "ws://localhost:7880",
                "devkey",
                "secret",
                Duration.ofMinutes(30)
        ));

        String token = issuer.issueJoinToken(
                RoomCode.from("ABC123"),
                ParticipantIdentity.from("22222222-2222-2222-2222-222222222222"),
                new ParticipantName("Juan"),
                Duration.ofMinutes(30)
        );

        assertThat(token).contains(".");
    }
}
