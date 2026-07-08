package edu.eci.uniplay.realtime.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DrawingDeltaTest {

    @Test
    void normalizesRoomCodeAndColor() {
        DrawingDelta delta = new DrawingDelta(
                "abc123",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                new CanvasPoint(0.1, 0.2),
                new CanvasPoint(0.3, 0.4),
                new StrokeStyle("#00ffaa", 4),
                Instant.parse("2026-07-07T12:00:00Z")
        );

        assertThat(delta.roomCode()).isEqualTo("ABC123");
        assertThat(delta.strokeStyle().color()).isEqualTo("#00FFAA");
    }

    @Test
    void rejectsInvalidRoomCode() {
        assertThatThrownBy(() -> new DrawingDelta(
                "ABC12",
                UUID.randomUUID(),
                new CanvasPoint(0.1, 0.2),
                new CanvasPoint(0.3, 0.4),
                new StrokeStyle("#000000", 4),
                Instant.now()
        ))
                .isInstanceOf(InvalidDrawingDeltaException.class)
                .hasMessage("roomCode must contain 6 uppercase alphanumeric characters");
    }

    @Test
    void rejectsCoordinatesOutsideCanvas() {
        assertThatThrownBy(() -> new CanvasPoint(1.2, 0.5))
                .isInstanceOf(InvalidDrawingDeltaException.class)
                .hasMessage("canvas coordinates must be normalized between 0 and 1");
    }

    @Test
    void rejectsInvalidStrokeColor() {
        assertThatThrownBy(() -> new StrokeStyle("blue", 4))
                .isInstanceOf(InvalidDrawingDeltaException.class)
                .hasMessage("stroke color must use #RRGGBB format");
    }

    @Test
    void rejectsInvalidStrokeWidth() {
        assertThatThrownBy(() -> new StrokeStyle("#000000", 41))
                .isInstanceOf(InvalidDrawingDeltaException.class)
                .hasMessage("stroke width must be between 1 and 40");
    }
}
