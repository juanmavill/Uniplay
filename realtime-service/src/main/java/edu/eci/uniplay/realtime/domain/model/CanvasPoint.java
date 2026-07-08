package edu.eci.uniplay.realtime.domain.model;

public record CanvasPoint(double x, double y) {

    public CanvasPoint {
        if (!isNormalizedCoordinate(x) || !isNormalizedCoordinate(y)) {
            throw new InvalidDrawingDeltaException("canvas coordinates must be normalized between 0 and 1");
        }
    }

    private static boolean isNormalizedCoordinate(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0 && value <= 1;
    }
}
