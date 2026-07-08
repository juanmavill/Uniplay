package edu.eci.uniplay.voice.application.service;

import java.time.Clock;
import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.ChangeMuteStateCommand;
import edu.eci.uniplay.voice.application.dto.MuteStateResult;
import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import edu.eci.uniplay.voice.application.port.in.ChangeMuteStateUseCase;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import edu.eci.uniplay.voice.domain.model.ParticipantIdentity;
import edu.eci.uniplay.voice.domain.model.RoomCode;

public class ChangeMuteStateService implements ChangeMuteStateUseCase {

    private final VoiceEventPublisher voiceEventPublisher;
    private final Clock clock;

    public ChangeMuteStateService(VoiceEventPublisher voiceEventPublisher, Clock clock) {
        this.voiceEventPublisher = voiceEventPublisher;
        this.clock = clock;
    }

    @Override
    public MuteStateResult changeMuteState(ChangeMuteStateCommand command) {
        RoomCode roomCode = RoomCode.from(command.roomCode());
        ParticipantIdentity participantIdentity = ParticipantIdentity.from(command.playerId());
        Instant changedAt = clock.instant();
        MuteStateChangedEvent event = new MuteStateChangedEvent(
                roomCode.value(),
                roomCode.voiceRoomName(),
                participantIdentity.asString(),
                command.muted(),
                changedAt
        );
        voiceEventPublisher.publishMuteStateChanged(event);
        return new MuteStateResult(
                event.roomCode(),
                event.voiceRoomName(),
                event.participantIdentity(),
                event.muted(),
                event.occurredAt()
        );
    }
}
