package edu.eci.uniplay.room.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RoomId(UUID value) {

    public RoomId {
        Objects.requireNonNull(value, "value is required");
    }

    public static RoomId newId() {
        return new RoomId(UUID.randomUUID());
    }
}
