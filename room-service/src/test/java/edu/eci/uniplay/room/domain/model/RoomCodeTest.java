package edu.eci.uniplay.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoomCodeTest {

    @Test
    void normalizesLowercaseCode() {
        RoomCode code = new RoomCode("ab12cd");

        assertThat(code.value()).isEqualTo("AB12CD");
    }

    @Test
    void rejectsCodesWithInvalidLength() {
        assertThatThrownBy(() -> new RoomCode("ABC12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("room code must contain 6 uppercase alphanumeric characters");
    }

    @Test
    void rejectsCodesWithSpecialCharacters() {
        assertThatThrownBy(() -> new RoomCode("ABC-12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("room code must contain 6 uppercase alphanumeric characters");
    }
}
