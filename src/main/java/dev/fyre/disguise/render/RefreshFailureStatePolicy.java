package dev.fyre.disguise.render;

/**
 * A failed transport send is ambiguous: the client may already have received
 * some or all of the outgoing disguise packets. Track such a pair for eventual
 * restoration whenever a disguise was being applied.
 */
public final class RefreshFailureStatePolicy {
    private RefreshFailureStatePolicy() { }

    public static boolean keepForRestore(boolean applyingDisguise, boolean alreadyApplied) {
        return applyingDisguise || alreadyApplied;
    }
}
