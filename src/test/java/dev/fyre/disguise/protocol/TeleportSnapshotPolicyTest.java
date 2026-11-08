package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class TeleportSnapshotPolicyTest {
    @Test public void rotationOnlyDoesNotInvalidate() {
        assertFalse(TeleportSnapshotPolicy.invalidates(false, 1, 2, 3, 1, 2, 3));
    }

    @Test public void movementAndWorldChangesAlwaysInvalidate() {
        assertTrue(TeleportSnapshotPolicy.invalidates(false, 1, 2, 3, 1, 2, 4));
        assertTrue(TeleportSnapshotPolicy.invalidates(false, 1, 2, 3, 1, 9, 3));
        assertTrue(TeleportSnapshotPolicy.invalidates(true, 1, 2, 3, 1, 2, 3));
    }
}
