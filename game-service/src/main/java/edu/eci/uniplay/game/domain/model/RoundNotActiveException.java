package edu.eci.uniplay.game.domain.model;

public class RoundNotActiveException extends RuntimeException {

    public RoundNotActiveException(String message) {
        super(message);
    }
}
