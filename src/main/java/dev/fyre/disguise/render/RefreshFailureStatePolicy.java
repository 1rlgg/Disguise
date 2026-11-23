package dev.fyre.disguise.render;


public final class RefreshFailureStatePolicy {
    private RefreshFailureStatePolicy() { }

    public static boolean keepForRestore(boolean applyingDisguise, boolean alreadyApplied) {
        return applyingDisguise || alreadyApplied;
    }
}
