package dev.fyre.disguise.protocol;

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
    @Test public void activeNonEmptyAddsCanBeRewritten() {
        assertTrue(PlayerInfoPacketPolicy.mayRewrite(false, true, true));
    }
}
