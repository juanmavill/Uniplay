package edu.eci.uniplay.voice.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantIdentityTest {

    @Test
    void acceptsUuidIdentity() {
        ParticipantIdentity identity = ParticipantIdentity.from("22222222-2222-2222-2222-222222222222");

        assertThat(identity.asString()).isEqualTo("22222222-2222-2222-2222-222222222222");
    }

    @Test
    void rejectsInvalidUuidIdentity() {
        assertThatThrownBy(() -> ParticipantIdentity.from("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
