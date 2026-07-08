package edu.eci.uniplay.room.application.port.out;

import edu.eci.uniplay.room.domain.model.Room;

public interface RoomRepository {

    boolean saveIfCodeAvailable(Room room);
}
