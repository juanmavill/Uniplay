package edu.eci.uniplay.room.application.port.in;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;

/**
 * Creates a UniPlay room and returns the join code that identifies it.
 */
public interface CreateRoomUseCase {

    RoomCreatedResult createRoom(CreateRoomCommand command);
}
