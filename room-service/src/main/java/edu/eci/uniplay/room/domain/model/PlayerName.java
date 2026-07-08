package edu.eci.uniplay.room.domain.model;

import java.util.Locale;
import java.util.Objects;

public record PlayerName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 30;

    public PlayerName {
        Objects.requireNonNull(value, "value is required");
        value = value.trim();

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("player name must contain between 2 and 30 characters");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PlayerName otherName)) {
            return false;
        }

        return value.toLowerCase(Locale.ROOT).equals(otherName.value.toLowerCase(Locale.ROOT));
    }

    @Override
    public int hashCode() {
        return value.toLowerCase(Locale.ROOT).hashCode();
    }
}
