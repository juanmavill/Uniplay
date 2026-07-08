package edu.eci.uniplay.room.domain.model;

import java.util.Objects;

public record Player(PlayerId id, PlayerName name) {

    public Player {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
    }

    public boolean hasName(PlayerName otherName) {
        return name.equals(otherName);
    }
}
