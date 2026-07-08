package edu.eci.uniplay.realtime.application.port.out;

import edu.eci.uniplay.realtime.application.dto.VoiceEventMessage;

public interface VoiceEventBroker {

    void sendVoiceEvent(VoiceEventMessage message);
}
