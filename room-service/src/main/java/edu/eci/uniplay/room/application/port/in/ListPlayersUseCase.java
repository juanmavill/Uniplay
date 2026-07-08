package edu.eci.uniplay.room.application.port.in;

import edu.eci.uniplay.room.application.dto.ListPlayersCommand;
import edu.eci.uniplay.room.application.dto.ListPlayersResult;

/**
 * Returns the current players in an existing UniPlay room.
 */
public interface ListPlayersUseCase {

    ListPlayersResult listPlayers(ListPlayersCommand command);
}
