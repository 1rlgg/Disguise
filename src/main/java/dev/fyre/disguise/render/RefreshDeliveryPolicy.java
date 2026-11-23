package dev.fyre.disguise.render;


public final class RefreshDeliveryPolicy {
    private RefreshDeliveryPolicy() { }

    public static boolean shouldSend(boolean currentlyDisguised, boolean previouslyApplied) {
        return currentlyDisguised || previouslyApplied;
    }
}
