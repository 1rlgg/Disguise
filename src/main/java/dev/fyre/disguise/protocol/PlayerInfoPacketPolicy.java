package dev.fyre.disguise.protocol;


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
}
