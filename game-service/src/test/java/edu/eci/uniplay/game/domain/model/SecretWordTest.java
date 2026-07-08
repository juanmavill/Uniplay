package edu.eci.uniplay.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretWordTest {

    @Test
    void matchesIgnoringCaseSpacesAndAccents() {
        SecretWord secretWord = new SecretWord("Árbol");

        assertThat(secretWord.matches(" arbol ")).isTrue();
    }

    @Test
    void rejectsBlankWord() {
        assertThatThrownBy(() -> new SecretWord(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret word is required");
    }
}
