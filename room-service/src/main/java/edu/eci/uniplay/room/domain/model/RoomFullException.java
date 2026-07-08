package edu.eci.uniplay.room.domain.model;

public class RoomFullException extends RuntimeException {

    public RoomFullException(RoomCode roomCode) {
        super("room is full: " + roomCode.value());
    }
}
