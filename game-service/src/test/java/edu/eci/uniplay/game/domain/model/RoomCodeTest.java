package edu.eci.uniplay.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomCodeTest {

    @Test
    void normalizesLowercaseCode() {
        assertThat(new RoomCode("abc123").value()).isEqualTo("ABC123");
    }

    @Test
    void rejectsInvalidCode() {
        assertThatThrownBy(() -> new RoomCode("ABC-12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 uppercase letters or numbers");
    }
}
