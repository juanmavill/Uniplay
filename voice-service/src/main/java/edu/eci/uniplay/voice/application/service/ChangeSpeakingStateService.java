package edu.eci.uniplay.voice.application.service;

import java.time.Clock;
import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.ChangeSpeakingStateCommand;
import edu.eci.uniplay.voice.application.dto.SpeakingStateResult;
import edu.eci.uniplay.voice.application.event.SpeakingStateChangedEvent;
import edu.eci.uniplay.voice.application.port.in.ChangeSpeakingStateUseCase;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.RoomCode;

public class ChangeSpeakingStateService implements ChangeSpeakingStateUseCase {

    private final VoiceEventPublisher voiceEventPublisher;
    private final Clock clock;

    public ChangeSpeakingStateService(VoiceEventPublisher voiceEventPublisher, Clock clock) {
        this.voiceEventPublisher = voiceEventPublisher;
        this.clock = clock;
    }

    @Override
    public SpeakingStateResult changeSpeakingState(ChangeSpeakingStateCommand command) {
        RoomCode roomCode = RoomCode.from(command.roomCode());
        ParticipantIdentity participantIdentity = ParticipantIdentity.from(command.playerId());
        Instant changedAt = clock.instant();
        SpeakingStateChangedEvent event = new SpeakingStateChangedEvent(
                roomCode.value(),
                roomCode.voiceRoomName(),
                participantIdentity.asString(),
                command.speaking(),
                changedAt
        );
        voiceEventPublisher.publishSpeakingStateChanged(event);
        return new SpeakingStateResult(
                event.roomCode(),
                event.voiceRoomName(),
                event.participantIdentity(),
                event.speaking(),
                event.occurredAt()
        );
    }
}
