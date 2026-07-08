package edu.eci.uniplay.room.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record RoomCode(String value) {

    private static final int EXPECTED_LENGTH = 6;
    private static final Pattern VALID_CODE_PATTERN = Pattern.compile("[A-Z0-9]{" + EXPECTED_LENGTH + "}");

    public RoomCode {
        Objects.requireNonNull(value, "value is required");
        value = value.trim().toUpperCase(Locale.ROOT);

        if (!VALID_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("room code must contain 6 uppercase alphanumeric characters");
        }
    }
}
