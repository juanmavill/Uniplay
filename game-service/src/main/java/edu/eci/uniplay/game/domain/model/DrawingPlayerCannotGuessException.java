package edu.eci.uniplay.game.domain.model;

public class DrawingPlayerCannotGuessException extends RuntimeException {

    public DrawingPlayerCannotGuessException(String message) {
        super(message);
    }
}
