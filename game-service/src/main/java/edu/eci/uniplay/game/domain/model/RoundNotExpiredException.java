package edu.eci.uniplay.game.domain.model;

public class RoundNotExpiredException extends RuntimeException {

    public RoundNotExpiredException(String message) {
        super(message);
    }
}
