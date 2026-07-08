package edu.eci.uniplay.room.application.exception;

public class RoomCodeGenerationException extends RuntimeException {

    public RoomCodeGenerationException(int attempts) {
        super("could not generate a unique room code after " + attempts + " attempts");
    }
}
