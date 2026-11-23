package dev.fyre.disguise.render;

import dev.fyre.disguise.render.refresh.RefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshFailureStatePolicyTest {
    @Test public void failedInitialAliasIsTrackedForRestoration() {
        assertTrue(RefreshPolicy.keepForRestore(true, false));
    }
    @Test public void failedUndisguiseKeepsExistingAppliedEntry() {
        assertTrue(RefreshPolicy.keepForRestore(false, true));
    }
    @Test public void noAliasAndNoPreviousEntryNeedNoRestoration() {
        assertFalse(RefreshPolicy.keepForRestore(false, false));
    }
}
