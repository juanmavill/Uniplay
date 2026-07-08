package edu.eci.uniplay.room.domain.model;

public class DuplicatePlayerException extends RuntimeException {

    public DuplicatePlayerException(PlayerName playerName) {
        super("player already joined room: " + playerName.value());
    }
}
