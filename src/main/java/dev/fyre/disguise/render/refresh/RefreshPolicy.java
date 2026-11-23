package dev.fyre.disguise.render.refresh;

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
}
