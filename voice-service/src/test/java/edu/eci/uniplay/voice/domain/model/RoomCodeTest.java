package edu.eci.uniplay.voice.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomCodeTest {

    @Test
    void normalizesRoomCodeForVoiceRoom() {
        RoomCode roomCode = RoomCode.from("abc123");

        assertThat(roomCode.value()).isEqualTo("ABC123");
        assertThat(roomCode.voiceRoomName()).isEqualTo("uniplay-ABC123");
    }

    @Test
    void rejectsInvalidRoomCode() {
        assertThatThrownBy(() -> RoomCode.from("A1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 uppercase");
    }
}
