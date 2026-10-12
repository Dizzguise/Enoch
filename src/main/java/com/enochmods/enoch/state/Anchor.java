package com.enochmods.enoch.state;

/** A player's feet position and view, independent of a particular server instance. */
public record Anchor(String dimension, double x, double y, double z, float yaw, float pitch) {
    public Anchor {
        if (dimension == null || !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90) {
            throw new IllegalArgumentException("Invalid Enoch anchor");
        }
    }
}
