package edu.eci.uniplay.room.application.port.out;

import edu.eci.uniplay.room.application.event.RoomCreatedEvent;

public interface DomainEventPublisher {

    void publishRoomCreated(RoomCreatedEvent event);
}
