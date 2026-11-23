package dev.fyre.disguise.render.restoration;

public final class ManualRestorationPolicy {
    private ManualRestorationPolicy() { }

    public static boolean allowed(boolean appliedByPlugin, boolean currentDisguise, boolean self) {

        return appliedByPlugin && !currentDisguise && !self;
    }
}
