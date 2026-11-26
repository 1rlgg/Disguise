package gg.rl1.disguise.protocol;

import gg.rl1.disguise.protocol.packet.PlayerInfoRewriter;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoPacketPolicyTest {
    @Test public void cancelledPacketsAreNeverRewritten() {
        assertFalse(PlayerInfoRewriter.mayRewrite(true, true, true));
    }
    @Test public void removalAndEmptyAddsAreUntouched() {
        assertFalse(PlayerInfoRewriter.mayRewrite(false, false, true));
        assertFalse(PlayerInfoRewriter.mayRewrite(false, true, false));
    }
    @Test public void displayNameUpdatesCannotLeakRealPlayerName() {
        assertTrue(PlayerInfoRewriter.mayRewrite(false, false, true, true));
        assertFalse(PlayerInfoRewriter.mayRewrite(true, false, true, true));
        assertFalse(PlayerInfoRewriter.mayRewrite(false, false, true, false));
        assertFalse(PlayerInfoRewriter.mayRewrite(false, false, false, true));
    }
    @Test public void activeNonEmptyAddsCanBeRewritten() {
        assertTrue(PlayerInfoRewriter.mayRewrite(false, true, true));
    }
}
