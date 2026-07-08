package edu.eci.uniplay.room.application.port.out;

import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import edu.eci.uniplay.room.application.event.PlayerJoinedEvent;

public interface DomainEventPublisher {

    void publishRoomCreated(RoomCreatedEvent event);

    void publishPlayerJoined(PlayerJoinedEvent event);
}
