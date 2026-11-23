package dev.fyre.disguise.render;

import dev.fyre.disguise.render.restoration.RestorationEligibility;

import org.junit.Test;
import static org.junit.Assert.*;

public class RestorationEligibilityTest {
    @Test public void onlyVisibleAppliedUndisguisedPairsNeedRestoration() {
        assertTrue(RestorationEligibility.canRestore(true, false, true, true, true, false));
        assertFalse(RestorationEligibility.canRestore(false, false, true, true, true, false));
        assertFalse(RestorationEligibility.canRestore(true, true, true, true, true, false));
        assertFalse(RestorationEligibility.canRestore(true, false, false, true, true, false));
        assertFalse(RestorationEligibility.canRestore(true, false, true, false, true, false));
        assertFalse(RestorationEligibility.canRestore(true, false, true, true, false, false));
        assertFalse(RestorationEligibility.canRestore(true, false, true, true, true, true));
    }
}
