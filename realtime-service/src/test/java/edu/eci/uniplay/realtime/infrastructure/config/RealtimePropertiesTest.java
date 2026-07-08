package edu.eci.uniplay.realtime.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class RealtimePropertiesTest {

    @Test
    void defaultsAllowedOriginsWhenMissing() {
        RealtimeProperties properties = new RealtimeProperties("/ws", null);

        assertThat(properties.allowedOrigins()).containsExactly("*");
    }

    @Test
    void rejectsBlankWebsocketEndpoint() {
        assertThatThrownBy(() -> new RealtimeProperties(" ", List.of("*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("websocketEndpoint is required");
    }
}
