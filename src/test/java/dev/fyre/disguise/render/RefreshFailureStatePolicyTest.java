package dev.fyre.disguise.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshFailureStatePolicyTest {
    @Test public void failedInitialAliasIsTrackedForRestoration() {
        assertTrue(RefreshFailureStatePolicy.keepForRestore(true, false));
    }
    @Test public void failedUndisguiseKeepsExistingAppliedEntry() {
        assertTrue(RefreshFailureStatePolicy.keepForRestore(false, true));
    }
    @Test public void noAliasAndNoPreviousEntryNeedNoRestoration() {
        assertFalse(RefreshFailureStatePolicy.keepForRestore(false, false));
    }
}
