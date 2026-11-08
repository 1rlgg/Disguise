package dev.fyre.disguise.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManualRestorationPolicyTest {
    @Test public void onlyRestoresPreviouslyAppliedUndisguisedNonSelfEntries() {
        assertTrue(ManualRestorationPolicy.allowed(true, false, false));
        assertFalse(ManualRestorationPolicy.allowed(false, false, false));
        assertFalse(ManualRestorationPolicy.allowed(true, true, false));
        assertFalse(ManualRestorationPolicy.allowed(true, false, true));
    }
}
