package edu.eci.uniplay.realtime.application.port.in;

import edu.eci.uniplay.realtime.application.dto.BroadcastDrawingCommand;
import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;

/**
 * Broadcasts one canvas drawing delta to clients subscribed to the room.
 */
public interface BroadcastDrawingUseCase {

    DrawingDeltaMessage broadcast(BroadcastDrawingCommand command);
}
