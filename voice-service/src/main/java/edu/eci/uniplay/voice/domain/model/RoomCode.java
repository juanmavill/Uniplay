package edu.eci.uniplay.voice.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record RoomCode(String value) {

    private static final Pattern VALID_CODE = Pattern.compile("^[A-Z0-9]{6}$");

    public RoomCode {
        if (value == null || !VALID_CODE.matcher(value).matches()) {
            throw new IllegalArgumentException("room code must contain 6 uppercase alphanumeric characters");
        }
    }

    public static RoomCode from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("room code is required");
        }
        return new RoomCode(value.toUpperCase(Locale.ROOT));
    }

    public String voiceRoomName() {
        return "uniplay-" + value;
    }
}
