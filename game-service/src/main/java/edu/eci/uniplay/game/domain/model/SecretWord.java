package edu.eci.uniplay.game.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public record SecretWord(String value) {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    public SecretWord {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("secret word is required");
        }

        value = value.trim();
    }

    public boolean matches(String answer) {
        return normalize(value).equals(normalize(answer));
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }
}
