package edu.eci.uniplay.room.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RoomPropertiesTest {

    @Test
    void acceptsValidProperties() {
        RoomProperties properties = new RoomProperties(21, 10, Duration.ofHours(2));

        assertThat(properties.maxPlayers()).isEqualTo(21);
        assertThat(properties.codeGenerationMaxAttempts()).isEqualTo(10);
        assertThat(properties.ttl()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void rejectsInvalidTtl() {
        assertThatThrownBy(() -> new RoomProperties(21, 10, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ttl must be positive");
    }
}
