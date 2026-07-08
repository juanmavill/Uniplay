package edu.eci.uniplay.room.application.port.out;

import java.util.Optional;

import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;

public interface RoomRepository {

    boolean saveIfCodeAvailable(Room room);

    Optional<Room> findByCode(RoomCode code);

    void save(Room room);
}
