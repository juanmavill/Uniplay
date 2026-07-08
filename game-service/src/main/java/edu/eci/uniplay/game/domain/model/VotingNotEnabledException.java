package edu.eci.uniplay.game.domain.model;

public class VotingNotEnabledException extends RuntimeException {

    public VotingNotEnabledException(String message) {
        super(message);
    }
}
