package edu.eci.uniplay.game.infrastructure.word;

import java.security.SecureRandom;
import java.util.List;

import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.SecretWord;

public class DefaultWordDeckProvider implements WordDeckProvider {

    private static final List<String> WORDS = List.of(
            "Biblioteca",
            "Campus",
            "Laboratorio",
            "Cafeteria",
            "Ingenieria",
            "Tablero"
    );

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public SecretWord nextWord(RoomCode roomCode) {
        return new SecretWord(WORDS.get(secureRandom.nextInt(WORDS.size())));
    }
}
