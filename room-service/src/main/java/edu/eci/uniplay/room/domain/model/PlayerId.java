package edu.eci.uniplay.room.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PlayerId(UUID value) {

    public PlayerId {
        Objects.requireNonNull(value, "value is required");
    }

    public static PlayerId newId() {
        return new PlayerId(UUID.randomUUID());
    }
}
