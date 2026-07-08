package edu.eci.uniplay.room.application.port.in;

import edu.eci.uniplay.room.application.dto.JoinRoomCommand;
import edu.eci.uniplay.room.application.dto.JoinRoomResult;

/**
 * Adds an anonymous player to an existing UniPlay room.
 */
public interface JoinRoomUseCase {

    JoinRoomResult joinRoom(JoinRoomCommand command);
}
