package edu.eci.uniplay.voice.application.port.out;

import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;

public interface VoiceEventPublisher {

    void publishMuteStateChanged(MuteStateChangedEvent event);
}
