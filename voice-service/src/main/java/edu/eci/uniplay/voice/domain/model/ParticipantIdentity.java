package edu.eci.uniplay.voice.domain.model;

import java.util.UUID;

public record ParticipantIdentity(UUID value) {

    public ParticipantIdentity {
        if (value == null) {
            throw new IllegalArgumentException("participant identity is required");
        }
    }

    public static ParticipantIdentity from(String value) {
        return new ParticipantIdentity(UUID.fromString(value));
    }

    public String asString() {
        return value.toString();
    }
}
