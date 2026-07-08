package edu.eci.uniplay.room.application.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String code) {
        super("room not found: " + code);
    }
}
