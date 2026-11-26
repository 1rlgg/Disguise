package gg.rl1.disguise.protocol;

import gg.rl1.disguise.protocol.entity.EntityRefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class TeleportSnapshotPolicyTest {
    @Test public void rotationOnlyDoesNotInvalidate() {
        assertFalse(EntityRefreshPolicy.invalidates(false, 1, 2, 3, 1, 2, 3));
    }

    @Test public void movementAndWorldChangesAlwaysInvalidate() {
        assertTrue(EntityRefreshPolicy.invalidates(false, 1, 2, 3, 1, 2, 4));
        assertTrue(EntityRefreshPolicy.invalidates(false, 1, 2, 3, 1, 9, 3));
        assertTrue(EntityRefreshPolicy.invalidates(true, 1, 2, 3, 1, 2, 3));
    }
}
