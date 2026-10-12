package com.enochmods.enoch.state;

public record PlayerState(Anchor anchor, long cooldownUntilMillis) {
    public static final PlayerState EMPTY = new PlayerState(null, 0);

    public PlayerState {
        if (cooldownUntilMillis < 0) throw new IllegalArgumentException("Negative cooldown");
    }

    public long remainingMillis(long now) {
        return Math.max(0, cooldownUntilMillis - now);
    }
}
