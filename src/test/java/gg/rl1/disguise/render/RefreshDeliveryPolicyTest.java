package gg.rl1.disguise.render;

import gg.rl1.disguise.render.refresh.RefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshDeliveryPolicyTest {
    @Test public void initialDisguiseRequiresDelivery() {
        assertTrue(RefreshPolicy.shouldSend(true, false));
    }

    @Test public void updatedDisguiseRequiresDelivery() {
        assertTrue(RefreshPolicy.shouldSend(true, true));
    }

    @Test public void undisguiseRequiresRestoration() {
        assertTrue(RefreshPolicy.shouldSend(false, true));
    }

    @Test public void transientSetThenClearWithoutAppliedEntryIsNoOp() {
        assertFalse(RefreshPolicy.shouldSend(false, false));
    }
}
