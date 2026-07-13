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
    private static final String CUSTOM_DECK = "CUSTOM";
    private static final int MIN_CUSTOM_WORDS = 3;
    private static final int MAX_CUSTOM_WORDS = 100;
    private static final int MAX_WORD_LENGTH = 40;

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
    public SecretWord nextWord(RoomCode roomCode, String deck, List<String> customWords) {
        String normalizedDeck = normalize(deck);
        List<String> words = CUSTOM_DECK.equals(normalizedDeck)
                ? validateCustomWords(customWords)
                : DECKS.get(normalizedDeck);
        if (words == null) {
            throw new IllegalArgumentException("word deck is not available");
        }
        return new SecretWord(words.get(secureRandom.nextInt(words.size())));
    }

    private List<String> validateCustomWords(List<String> customWords) {
        if (customWords == null) {
            throw new IllegalArgumentException("custom word deck requires words");
        }
        List<String> normalizedWords = customWords.stream()
                .filter(word -> word != null && !word.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedWords.size() < MIN_CUSTOM_WORDS || normalizedWords.size() > MAX_CUSTOM_WORDS) {
            throw new IllegalArgumentException("custom word deck must contain between 3 and 100 unique words");
        }
        if (normalizedWords.stream().anyMatch(word -> word.length() > MAX_WORD_LENGTH)) {
            throw new IllegalArgumentException("custom words must contain at most 40 characters");
        }
        return normalizedWords;
    }

    private String normalize(String deck) {
        if (deck == null || deck.isBlank()) {
            return DEFAULT_DECK;
        }
        return deck.trim().toUpperCase(Locale.ROOT);
    }
}
