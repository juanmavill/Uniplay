package edu.eci.uniplay.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlayerNameTest {

    @Test
    void trimsPlayerName() {
        PlayerName playerName = new PlayerName(" Ana ");

        assertThat(playerName.value()).isEqualTo("Ana");
    }

    @Test
    void comparesNamesIgnoringCase() {
        assertThat(new PlayerName("Ana")).isEqualTo(new PlayerName("ana"));
    }

    @Test
    void rejectsTooShortName() {
        assertThatThrownBy(() -> new PlayerName("A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("player name must contain between 2 and 30 characters");
    }
}
