package dev.fyre.disguise.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

public class EntityRefreshPolicyTest {
    @Test public void permitsOnlySafeAndTrackedPlayers() {
        assertTrue(EntityRefreshPolicy.permitted(true, true, true, false, false, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(false, true, true, false, false, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, false, true, false, false, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, false, false, false, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, true, true, false, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, true, false, true, false, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, true, false, false, true, true, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, true, false, false, false, false, true));
        assertFalse(EntityRefreshPolicy.permitted(true, true, true, false, false, false, true, false));
    }

    @Test public void rejectsInvalidAndOutOfRangeDistance() {
        assertTrue(EntityRefreshPolicy.withinTrackingRange(256.0D * 256.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(257.0D * 257.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(Double.NaN, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(Double.POSITIVE_INFINITY, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(-1.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(1.0D, 0));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(100.0D * 100.0D, 4));
    }
}
