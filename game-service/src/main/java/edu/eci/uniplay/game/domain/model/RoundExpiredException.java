package edu.eci.uniplay.game.domain.model;

public class RoundExpiredException extends RuntimeException {

    public RoundExpiredException(String message) {
        super(message);
    }
}
