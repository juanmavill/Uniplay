package edu.eci.uniplay.voice.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import edu.eci.uniplay.voice.application.dto.ChangeMuteStateCommand;
import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChangeMuteStateServiceTest {

    private final VoiceEventPublisher voiceEventPublisher = mock(VoiceEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishesMuteStateChangedEvent() {
        ChangeMuteStateService service = new ChangeMuteStateService(voiceEventPublisher, clock);

        var result = service.changeMuteState(new ChangeMuteStateCommand(
                "ABC123",
                "22222222-2222-2222-2222-222222222222",
                true
        ));

        ArgumentCaptor<MuteStateChangedEvent> captor = ArgumentCaptor.forClass(MuteStateChangedEvent.class);
        verify(voiceEventPublisher).publishMuteStateChanged(captor.capture());
        assertThat(captor.getValue().voiceRoomName()).isEqualTo("uniplay-ABC123");
        assertThat(captor.getValue().muted()).isTrue();
        assertThat(result.changedAt()).isEqualTo(Instant.parse("2026-07-07T12:00:00Z"));
    }
}
