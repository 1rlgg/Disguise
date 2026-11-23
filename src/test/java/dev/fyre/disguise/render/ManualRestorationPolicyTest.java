package dev.fyre.disguise.render;

import dev.fyre.disguise.render.restoration.ManualRestorationPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManualRestorationPolicyTest {
    @Test public void neverQueuesRepairForAnActiveAliasEvenIfItsOldProfileWasApplied() {
        assertFalse(ManualRestorationPolicy.allowed(true, true, false));
        assertFalse(ManualRestorationPolicy.allowed(false, true, false));
    }

    @Test public void onlyRestoresPreviouslyAppliedUndisguisedNonSelfEntries() {
        assertTrue(ManualRestorationPolicy.allowed(true, false, false));
        assertFalse(ManualRestorationPolicy.allowed(false, false, false));
        assertFalse(ManualRestorationPolicy.allowed(true, true, false));
        assertFalse(ManualRestorationPolicy.allowed(true, false, true));
    }
}
