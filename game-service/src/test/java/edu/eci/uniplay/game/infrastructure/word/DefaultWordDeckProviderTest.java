package edu.eci.uniplay.game.infrastructure.word;

import edu.eci.uniplay.game.domain.model.RoomCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWordDeckProviderTest {

    @Test
    void selectsWordFromRequestedAcademicDeck() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        String word = provider.nextWord(new RoomCode("ABC123"), "sistemas", null).value();

        assertThat(word).isIn("Algoritmo", "Servidor", "Compilador", "Variable", "Repositorio", "Arquitectura");
    }

    @Test
    void fallsBackToGeneralDeckWhenDeckIsMissing() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        String word = provider.nextWord(new RoomCode("ABC123"), null, null).value();

        assertThat(word).isNotBlank();
    }

    @Test
    void rejectsUnknownDeck() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        assertThatThrownBy(() -> provider.nextWord(new RoomCode("ABC123"), "historia", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("word deck");
    }

    @Test
    void selectsWordFromValidatedCustomDeck() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        String word = provider.nextWord(
                new RoomCode("ABC123"),
                "custom",
                java.util.List.of("Dragon", "Castillo", "Astronauta")
        ).value();

        assertThat(word).isIn("Dragon", "Castillo", "Astronauta");
    }

    @Test
    void rejectsCustomDeckWithFewerThanThreeUniqueWords() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        assertThatThrownBy(() -> provider.nextWord(
                new RoomCode("ABC123"),
                "CUSTOM",
                java.util.List.of("Dragon", "Dragon", "Castillo")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 3 and 100 unique words");
    }

    @Test
    void rejectsCustomWordsLongerThanFortyCharacters() {
        DefaultWordDeckProvider provider = new DefaultWordDeckProvider();

        assertThatThrownBy(() -> provider.nextWord(
                new RoomCode("ABC123"),
                "CUSTOM",
                java.util.List.of("Dragon", "Castillo", "x".repeat(41))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 40 characters");
    }
}
