package dev.fyre.disguise.render;

/** Bound expensive experimental world respawns without throttling tab fallback. */
public final class RefreshBatchPolicy {
    private RefreshBatchPolicy() { }

    public static int size(int configuredLimit, boolean worldRefreshEnabled) {
        if (configuredLimit < 1)
            throw new IllegalArgumentException("Refresh limit must be positive");
        return worldRefreshEnabled ? Math.min(configuredLimit, 8) : configuredLimit;
    }
}
