package dev.fyre.disguise.render;

import dev.fyre.disguise.render.refresh.RefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class RestorationEligibilityTest {
    @Test public void onlyVisibleAppliedUndisguisedPairsNeedRestoration() {
        assertTrue(RefreshPolicy.canRestore(true, false, true, true, true, false));
        assertFalse(RefreshPolicy.canRestore(false, false, true, true, true, false));
        assertFalse(RefreshPolicy.canRestore(true, true, true, true, true, false));
        assertFalse(RefreshPolicy.canRestore(true, false, false, true, true, false));
        assertFalse(RefreshPolicy.canRestore(true, false, true, false, true, false));
        assertFalse(RefreshPolicy.canRestore(true, false, true, true, false, false));
        assertFalse(RefreshPolicy.canRestore(true, false, true, true, true, true));
    }
}
