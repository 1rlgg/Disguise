package dev.fyre.disguise.render;

/** Admission for explicit original-profile restoration on a test server. */
public final class ManualRestorationPolicy {
    private ManualRestorationPolicy() { }

    public static boolean allowed(boolean appliedByPlugin, boolean currentDisguise, boolean self) {
        // Avoid injecting a vanilla tab entry for unrelated/vanished players.
        return appliedByPlugin && !currentDisguise && !self;
    }
}
