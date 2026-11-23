package dev.fyre.disguise.protocol.entity;

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
