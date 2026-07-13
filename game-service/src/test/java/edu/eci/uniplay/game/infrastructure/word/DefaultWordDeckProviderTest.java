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
}
