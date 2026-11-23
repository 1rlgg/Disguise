package dev.fyre.disguise.protocol.entity;

public final class EntityRefreshPolicy {
    private EntityRefreshPolicy() { }

    public static boolean permitted(boolean sameWorld, boolean visible, boolean online,
                                    boolean dead, boolean sleeping, boolean mounted,
                                    boolean inTrackingRange, boolean hasMatchingSpawn) {
        return permitted(sameWorld, visible, online, dead, sleeping, mounted,
                false, inTrackingRange, hasMatchingSpawn);
    }

    public static boolean permitted(boolean sameWorld, boolean visible, boolean online,
                                    boolean dead, boolean sleeping, boolean mounted,
                                    boolean spectator, boolean inTrackingRange,
                                    boolean hasMatchingSpawn) {
        return sameWorld && visible && online && !dead && !sleeping && !mounted
                && !spectator && inTrackingRange && hasMatchingSpawn;
    }

    public static boolean withinTrackingRange(double distanceSquared, int viewDistanceChunks) {
        if (Double.isNaN(distanceSquared) || Double.isInfinite(distanceSquared) || distanceSquared < 0
                || viewDistanceChunks < 1) return false;
        double limit = Math.min(viewDistanceChunks * 16.0D, 48.0D);
        return distanceSquared <= limit * limit;
    }
}
