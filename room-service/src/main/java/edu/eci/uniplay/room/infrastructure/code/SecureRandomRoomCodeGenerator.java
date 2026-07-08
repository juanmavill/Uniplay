package edu.eci.uniplay.room.infrastructure.code;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import edu.eci.uniplay.room.application.port.out.RoomCodeGenerator;
import edu.eci.uniplay.room.domain.model.RoomCode;

public class SecureRandomRoomCodeGenerator implements RoomCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    private final RandomGenerator randomGenerator;

    public SecureRandomRoomCodeGenerator() {
        this(new SecureRandom());
    }

    SecureRandomRoomCodeGenerator(RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    @Override
    public RoomCode generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int position = 0; position < CODE_LENGTH; position++) {
            int index = randomGenerator.nextInt(ALPHABET.length());
            code.append(ALPHABET.charAt(index));
        }

        return new RoomCode(code.toString());
    }
}
