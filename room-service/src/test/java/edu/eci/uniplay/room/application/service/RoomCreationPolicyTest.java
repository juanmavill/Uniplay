package edu.eci.uniplay.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoomCreationPolicyTest {

    @Test
    void usesDefaultMaxPlayersWhenRequestDoesNotProvideOne() {
        RoomCreationPolicy policy = new RoomCreationPolicy(21, 3);

        assertThat(policy.resolveMaxPlayers(null)).isEqualTo(21);
    }

    @Test
    void usesRequestedMaxPlayersWhenProvided() {
        RoomCreationPolicy policy = new RoomCreationPolicy(21, 3);

        assertThat(policy.resolveMaxPlayers(8)).isEqualTo(8);
    }

    @Test
    void rejectsInvalidRequestedMaxPlayers() {
        RoomCreationPolicy policy = new RoomCreationPolicy(21, 3);

        assertThatThrownBy(() -> policy.resolveMaxPlayers(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPlayers must be at least 2");
    }

    @Test
    void rejectsInvalidDefaultMaxPlayers() {
        assertThatThrownBy(() -> new RoomCreationPolicy(1, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("defaultMaxPlayers must be at least 2");
    }

    @Test
    void rejectsInvalidCodeGenerationAttempts() {
        assertThatThrownBy(() -> new RoomCreationPolicy(21, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxCodeGenerationAttempts must be at least 1");
    }
}
