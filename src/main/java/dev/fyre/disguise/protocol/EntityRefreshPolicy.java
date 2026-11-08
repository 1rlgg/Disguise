package dev.fyre.disguise.protocol;

/**
 * Pure admission checks for the experimental client entity destroy/respawn.
 * The actual render engine supplies observed player state on Bukkit's main thread.
 */
public final class EntityRefreshPolicy {
    private EntityRefreshPolicy() { }

    /**
     * Skip unsupported or unsafe states rather than guessing what packets a
     * legacy client needs to reconstruct sleeping, mounted or dead players.
     */
    public static boolean permitted(boolean sameWorld, boolean visible, boolean online,
                                    boolean dead, boolean sleeping, boolean mounted,
                                    boolean inTrackingRange, boolean hasMatchingSpawn) {
        return sameWorld && visible && online && !dead && !sleeping && !mounted
                && inTrackingRange && hasMatchingSpawn;
    }

    /** Conservative ceiling; view-distance is in chunks, not blocks. */
    public static boolean withinTrackingRange(double distanceSquared, int viewDistanceChunks) {
        if (Double.isNaN(distanceSquared) || Double.isInfinite(distanceSquared) || distanceSquared < 0
                || viewDistanceChunks < 1) return false;
        double limit = Math.min(viewDistanceChunks, 16) * 16.0D;
        return distanceSquared <= limit * limit;
    }
}
