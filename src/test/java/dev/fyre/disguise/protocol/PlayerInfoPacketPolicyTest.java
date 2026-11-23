package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.packet.PlayerInfoPacketPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoPacketPolicyTest {
    @Test public void cancelledPacketsAreNeverRewritten() {
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(true, true, true));
    }
    @Test public void removalAndEmptyAddsAreUntouched() {
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(false, false, true));
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(false, true, false));
    }
    @Test public void displayNameUpdatesCannotLeakRealPlayerName() {
        assertTrue(PlayerInfoPacketPolicy.mayRewrite(false, false, true, true));
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(true, false, true, true));
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(false, false, true, false));
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(false, false, false, true));
    }
    @Test public void activeNonEmptyAddsCanBeRewritten() {
        assertTrue(PlayerInfoPacketPolicy.mayRewrite(false, true, true));
    }
}
