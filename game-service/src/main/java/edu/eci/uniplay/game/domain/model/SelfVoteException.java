package edu.eci.uniplay.game.domain.model;

public class SelfVoteException extends RuntimeException {

    public SelfVoteException(String message) {
        super(message);
    }
}
