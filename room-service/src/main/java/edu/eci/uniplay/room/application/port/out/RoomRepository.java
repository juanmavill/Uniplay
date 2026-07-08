package edu.eci.uniplay.room.application.port.out;

import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;

public interface RoomRepository {

    boolean existsByCode(RoomCode code);

    void save(Room room);
}
