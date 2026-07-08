package edu.eci.uniplay.voice.application.port.out;

import edu.eci.uniplay.voice.application.event.MuteStateChangedEvent;
import edu.eci.uniplay.voice.application.event.SpeakingStateChangedEvent;

public interface VoiceEventPublisher {

    void publishMuteStateChanged(MuteStateChangedEvent event);

    void publishSpeakingStateChanged(SpeakingStateChangedEvent event);
}
