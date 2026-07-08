package edu.eci.uniplay.game.domain.model;

public class RoundAlreadyActiveException extends RuntimeException {

    public RoundAlreadyActiveException(String message) {
        super(message);
    }
}
