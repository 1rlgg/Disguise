package dev.fyre.disguise.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshDeliveryPolicyTest {
    @Test public void initialDisguiseRequiresDelivery() {
        assertTrue(RefreshDeliveryPolicy.shouldSend(true, false));
    }

    @Test public void updatedDisguiseRequiresDelivery() {
        assertTrue(RefreshDeliveryPolicy.shouldSend(true, true));
    }

    @Test public void undisguiseRequiresRestoration() {
        assertTrue(RefreshDeliveryPolicy.shouldSend(false, true));
    }

    @Test public void transientSetThenClearWithoutAppliedEntryIsNoOp() {
        assertFalse(RefreshDeliveryPolicy.shouldSend(false, false));
    }
}
