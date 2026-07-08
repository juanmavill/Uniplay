package edu.eci.uniplay.room.infrastructure.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import edu.eci.uniplay.room.domain.model.RoomCode;
import org.junit.jupiter.api.Test;

class SecureRandomRoomCodeGeneratorTest {

    @Test
    void generatesSixCharacterAlphanumericCode() {
        SecureRandomRoomCodeGenerator generator = new SecureRandomRoomCodeGenerator(new Random(1));

        RoomCode code = generator.generate();

        assertThat(code.value()).matches("[A-Z0-9]{6}");
    }
}
