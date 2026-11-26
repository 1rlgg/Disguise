package gg.rl1.disguise.render.refresh;

public final class RefreshPolicy {
    private RefreshPolicy() {
    }

    public static boolean mayAttempt(boolean retryQuarantined) {
        return !retryQuarantined;
    }

    public static boolean shouldSend(boolean currentlyDisguised, boolean previouslyApplied) {
        return currentlyDisguised || previouslyApplied;
    }

    public static boolean keepForRestore(boolean applyingDisguise, boolean alreadyApplied) {
        return applyingDisguise || alreadyApplied;
    }
    public static int batchSize(int configuredLimit, boolean worldRefreshEnabled) {
        if (configuredLimit < 1)
            throw new IllegalArgumentException("Refresh limit must be positive");
        return worldRefreshEnabled ? Math.min(configuredLimit, 8) : configuredLimit;
    }
    public static boolean manualRestorationAllowed(boolean appliedByPlugin, boolean currentDisguise, boolean self) {

        return appliedByPlugin && !currentDisguise && !self;
    }
    public static boolean canRestore(boolean applied, boolean disguised,
                                     boolean viewerOnline, boolean subjectOnline,
                                     boolean visible, boolean self) {
        return applied && !disguised && viewerOnline && subjectOnline
                && visible && !self;
    }
}
