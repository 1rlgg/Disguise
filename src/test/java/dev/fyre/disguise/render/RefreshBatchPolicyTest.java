package dev.fyre.disguise.render;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshBatchPolicyTest {
    @Test public void expensiveWorldRefreshUsesEightPairLimit() {
        assertEquals(8, RefreshBatchPolicy.size(32, true));
        assertEquals(3, RefreshBatchPolicy.size(3, true));
    }

    @Test public void disabledWorldRefreshRestoresFullTabOnlyThroughput() {
        assertEquals(32, RefreshBatchPolicy.size(32, false));
        assertEquals(128, RefreshBatchPolicy.size(128, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesNonpositiveTickBudget() {
        RefreshBatchPolicy.size(0, false);
    }
}
