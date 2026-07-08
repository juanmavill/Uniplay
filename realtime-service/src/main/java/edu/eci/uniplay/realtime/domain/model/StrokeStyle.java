package edu.eci.uniplay.realtime.domain.model;

import java.util.Locale;
import java.util.Objects;

public record StrokeStyle(String color, double width) {

    private static final double MIN_WIDTH = 1.0;
    private static final double MAX_WIDTH = 40.0;

    public StrokeStyle {
        color = normalizeColor(color);

        if (Double.isNaN(width) || Double.isInfinite(width) || width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new InvalidDrawingDeltaException("stroke width must be between 1 and 40");
        }
    }

    private static String normalizeColor(String color) {
        Objects.requireNonNull(color, "color is required");
        String normalizedColor = color.trim().toUpperCase(Locale.ROOT);

        if (!normalizedColor.matches("#[0-9A-F]{6}")) {
            throw new InvalidDrawingDeltaException("stroke color must use #RRGGBB format");
        }

        return normalizedColor;
    }
}
