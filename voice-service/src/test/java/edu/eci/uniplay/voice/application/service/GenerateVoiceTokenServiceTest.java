package edu.eci.uniplay.voice.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import edu.eci.uniplay.voice.application.dto.GenerateVoiceTokenCommand;
import edu.eci.uniplay.voice.application.port.out.VoiceTokenIssuer;
import edu.eci.uniplay.voice.infrastructure.config.VoiceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateVoiceTokenServiceTest {

    private final VoiceTokenIssuer voiceTokenIssuer = mock(VoiceTokenIssuer.class);
    private final VoiceProperties voiceProperties = new VoiceProperties(
            "ws://livekit:7880",
            "ws://localhost:7880",
            "devkey",
            "secret",
            Duration.ofMinutes(30)
    );
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void generatesTokenForRoomParticipant() {
        when(voiceTokenIssuer.issueJoinToken(any(), any(), any(), eq(Duration.ofMinutes(30))))
                .thenReturn("jwt-token");
        GenerateVoiceTokenService service = new GenerateVoiceTokenService(voiceTokenIssuer, voiceProperties, clock);

        var result = service.generateToken(new GenerateVoiceTokenCommand(
                "abc123",
                "22222222-2222-2222-2222-222222222222",
                "Juan"
        ));

        assertThat(result.roomCode()).isEqualTo("ABC123");
        assertThat(result.voiceRoomName()).isEqualTo("uniplay-ABC123");
        assertThat(result.participantIdentity()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(result.livekitUrl()).isEqualTo("ws://localhost:7880");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-07-07T12:30:00Z"));
    }

    @Test
    void rejectsInvalidRoomCode() {
        GenerateVoiceTokenService service = new GenerateVoiceTokenService(voiceTokenIssuer, voiceProperties, clock);

        assertThatThrownBy(() -> service.generateToken(new GenerateVoiceTokenCommand(
                "bad",
                "22222222-2222-2222-2222-222222222222",
                "Juan"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("room code");
    }
}
