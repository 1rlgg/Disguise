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

    @Test public void neverManuallyRespawnsSpectatorPlayers() {
        assertFalse(EntityRefreshPolicy.permitted(true, true, true,
                false, false, false, true, true, true));
        assertTrue(EntityRefreshPolicy.permitted(true, true, true,
                false, false, false, false, true, true));
    }

    @Test public void rejectsInvalidAndOutOfRangeDistance() {
        assertTrue(EntityRefreshPolicy.withinTrackingRange(48.0D * 48.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(49.0D * 49.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(256.0D * 256.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(Double.NaN, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(Double.POSITIVE_INFINITY, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(-1.0D, 16));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(1.0D, 0));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(100.0D * 100.0D, 4));
        assertTrue(EntityRefreshPolicy.withinTrackingRange(16.0D * 16.0D, 1));
        assertFalse(EntityRefreshPolicy.withinTrackingRange(17.0D * 17.0D, 1));
    }
}
