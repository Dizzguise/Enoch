package com.enochmods.enoch.state;

/** Applies to future returns; existing player cooldown deadlines are never rewritten. */
public record WorldConfig(int cooldownSeconds) {
    public static final int MAX_COOLDOWN_SECONDS = 3600;
    public static final WorldConfig DEFAULT = new WorldConfig(10);

    public WorldConfig {
        if (cooldownSeconds < 0 || cooldownSeconds > MAX_COOLDOWN_SECONDS) {
            throw new IllegalArgumentException("Cooldown must be 0..3600 seconds");
        }
    }

    public long cooldownMillis() { return cooldownSeconds * 1000L; }
}
