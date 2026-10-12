package net.historicalexperience.client.hud;

/**
 * A boolean that reports whether it has just changed state.
 *
 * <p>PORTED (trimmed) from Nostalgic Tweaks {@code util/common/data/FlagHolder.java} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md). Only the members the heads-up display engine uses are carried over: the pose stack
 * has to be popped exactly as many times as it was pushed, and each element needs to remember whether IT
 * pushed, which is what {@code ifEnabledThenDisable} answers. Upstream's listener plumbing is not ported.
 */
public final class FlagHolder {
    private boolean enabled;

    private FlagHolder(boolean enabled) {
        this.enabled = enabled;
    }

    public static FlagHolder off() {
        return new FlagHolder(false);
    }

    public boolean get() {
        return this.enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    /** Upstream: enables and reports whether this call is the one that changed the state. */
    public boolean ifDisabledThenEnable() {
        if (this.enabled) {
            return false;
        }
        this.enabled = true;
        return true;
    }

    /** Upstream: disables and reports whether this call is the one that changed the state. */
    public boolean ifEnabledThenDisable() {
        if (!this.enabled) {
            return false;
        }
        this.enabled = false;
        return true;
    }
}
