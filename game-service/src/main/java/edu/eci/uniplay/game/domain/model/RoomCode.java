package edu.eci.uniplay.game.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record RoomCode(String value) {

    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("[A-Z0-9]{6}");

    public RoomCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("room code is required");
        }

        value = value.trim().toUpperCase(Locale.ROOT);

        if (!ROOM_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("room code must contain 6 uppercase letters or numbers");
        }
    }
}
