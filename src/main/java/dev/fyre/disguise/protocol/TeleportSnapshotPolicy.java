package dev.fyre.disguise.protocol;

/**
 * Only movement/world changes invalidate tracked entity spawn snapshots.
 * Legacy Spigot fires teleport events for some look-only changes; those must
 * not flush per-viewer tracking caches.
 */
public final class TeleportSnapshotPolicy {
    private TeleportSnapshotPolicy() { }

    public static boolean invalidates(boolean worldChanged,
                                      double fromX, double fromY, double fromZ,
                                      double toX, double toY, double toZ) {
        return worldChanged || Double.compare(fromX, toX) != 0
                || Double.compare(fromY, toY) != 0
                || Double.compare(fromZ, toZ) != 0;
    }
}
