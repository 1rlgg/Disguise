package dev.fyre.disguise.render;

/**
 * A queue entry may become obsolete before delivery. Avoid any player-info
 * remove/add when neither a disguise nor a previously applied alias exists.
 */
public final class RefreshDeliveryPolicy {
    private RefreshDeliveryPolicy() { }

    public static boolean shouldSend(boolean currentlyDisguised, boolean previouslyApplied) {
        return currentlyDisguised || previouslyApplied;
    }
}
