package gg.rl1.disguise.render;

import gg.rl1.disguise.render.refresh.RefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManualRestorationPolicyTest {
    @Test public void neverQueuesRepairForAnActiveAliasEvenIfItsOldProfileWasApplied() {
        assertFalse(RefreshPolicy.manualRestorationAllowed(true, true, false));
        assertFalse(RefreshPolicy.manualRestorationAllowed(false, true, false));
    }

    @Test public void onlyRestoresPreviouslyAppliedUndisguisedNonSelfEntries() {
        assertTrue(RefreshPolicy.manualRestorationAllowed(true, false, false));
        assertFalse(RefreshPolicy.manualRestorationAllowed(false, false, false));
        assertFalse(RefreshPolicy.manualRestorationAllowed(true, true, false));
        assertFalse(RefreshPolicy.manualRestorationAllowed(true, false, true));
    }
}
