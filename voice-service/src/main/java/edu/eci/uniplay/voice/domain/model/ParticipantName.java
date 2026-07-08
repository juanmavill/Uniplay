package edu.eci.uniplay.voice.domain.model;

public record ParticipantName(String value) {

    private static final int MAX_LENGTH = 40;

    public ParticipantName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("participant name is required");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("participant name must contain at most 40 characters");
        }
    }
}
