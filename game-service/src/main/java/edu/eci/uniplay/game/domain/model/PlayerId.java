package edu.eci.uniplay.game.domain.model;

import java.util.UUID;

public record PlayerId(UUID value) {

    public PlayerId {
        if (value == null) {
            throw new IllegalArgumentException("player id is required");
        }
    }
}
