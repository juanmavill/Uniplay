package edu.eci.uniplay.realtime.application.port.out;

import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;

public interface DrawingMessageBroker {

    void sendDrawingDelta(DrawingDeltaMessage message);
}
