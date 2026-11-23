package dev.fyre.disguise.protocol.packet;

public final class PlayerInfoPacketPolicy {
    private PlayerInfoPacketPolicy() { }

    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean hasEntries) {

        return mayRewrite(packetCancelled, addPlayerAction, false, hasEntries);
    }

    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean updateDisplayNameAction, boolean hasEntries) {
        return !packetCancelled && (addPlayerAction || updateDisplayNameAction) && hasEntries;
    }
    public static boolean mayRewrite(java.util.UUID viewer, java.util.UUID subject) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        java.util.Objects.requireNonNull(subject, "subject");
        return !viewer.equals(subject);
    }
}
