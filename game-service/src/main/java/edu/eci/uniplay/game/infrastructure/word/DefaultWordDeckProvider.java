package edu.eci.uniplay.game.infrastructure.word;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.SecretWord;

public class DefaultWordDeckProvider implements WordDeckProvider {

    private static final String DEFAULT_DECK = "GENERAL";

    private static final Map<String, List<String>> DECKS = Map.of(
            DEFAULT_DECK, List.of(
            "Biblioteca",
            "Campus",
            "Laboratorio",
            "Cafeteria",
            "Ingenieria",
            "Tablero"
            ),
            "MATEMATICAS", List.of(
                    "Integral",
                    "Vector",
                    "Matriz",
                    "Parabola",
                    "Derivada",
                    "Geometria"
            ),
            "SISTEMAS", List.of(
                    "Algoritmo",
                    "Servidor",
                    "Compilador",
                    "Variable",
                    "Repositorio",
                    "Arquitectura"
            ),
            "FISICA", List.of(
                    "Gravedad",
                    "Energia",
                    "Circuito",
                    "Fuerza",
                    "Optica",
                    "Movimiento"
            )
    );

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public SecretWord nextWord(RoomCode roomCode, String deck) {
        List<String> words = DECKS.get(normalize(deck));
        if (words == null) {
            throw new IllegalArgumentException("word deck is not available");
        }
        return new SecretWord(words.get(secureRandom.nextInt(words.size())));
    }

    private String normalize(String deck) {
        if (deck == null || deck.isBlank()) {
            return DEFAULT_DECK;
        }
        return deck.trim().toUpperCase(Locale.ROOT);
    }
}
