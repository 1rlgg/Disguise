package gg.rl1.disguise.render;

import gg.rl1.disguise.render.refresh.RefreshPolicy;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshBatchPolicyTest {
    @Test public void expensiveWorldRefreshUsesEightPairLimit() {
        assertEquals(8, RefreshPolicy.batchSize(32, true));
        assertEquals(3, RefreshPolicy.batchSize(3, true));
    }

    @Test public void disabledWorldRefreshRestoresFullTabOnlyThroughput() {
        assertEquals(32, RefreshPolicy.batchSize(32, false));
        assertEquals(128, RefreshPolicy.batchSize(128, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesNonpositiveTickBudget() {
        RefreshPolicy.batchSize(0, false);
    }
}
