package edu.eci.uniplay.realtime.application.port.out;

import edu.eci.uniplay.realtime.application.dto.RoundEventMessage;

public interface RoundEventBroker {

    void sendRoundEvent(RoundEventMessage message);
}
