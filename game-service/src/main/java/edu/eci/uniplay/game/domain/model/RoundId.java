package edu.eci.uniplay.game.domain.model;

import java.util.UUID;

public record RoundId(UUID value) {

    public RoundId {
        if (value == null) {
            throw new IllegalArgumentException("round id is required");
        }
    }
}
