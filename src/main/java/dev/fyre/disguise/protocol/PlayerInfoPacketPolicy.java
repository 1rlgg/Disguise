package dev.fyre.disguise.protocol;

/** Read-only admission policy for third-party-modified player-info packets. */
public final class PlayerInfoPacketPolicy {
    private PlayerInfoPacketPolicy() { }

    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean hasEntries) {
        // A higher-priority plugin can suppress its own tracking packets;
        // never mutate those cancelled payloads.
        return mayRewrite(packetCancelled, addPlayerAction, false, hasEntries);
    }

    /** Player-list display updates must not undo a previously sent alias. */
    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean updateDisplayNameAction, boolean hasEntries) {
        return !packetCancelled && (addPlayerAction || updateDisplayNameAction) && hasEntries;
    }
}
