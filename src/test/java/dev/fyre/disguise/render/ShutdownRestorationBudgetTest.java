package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShutdownRestorationBudgetTest {
    @Test public void respectsBudgetAndStableInsertionOrder() {
        List<RefreshQueue.Pair> applied = new ArrayList<RefreshQueue.Pair>();
        UUID viewer = UUID.randomUUID();
        for (int i = 0; i < 1000; i++) applied.add(
                new RefreshQueue.Pair(viewer, new UUID(i + 1, 42L)));
        ShutdownRestorationBudget budget = new ShutdownRestorationBudget(256);
        List<RefreshQueue.Pair> subset = budget.select(applied);
        assertEquals(256, subset.size());
        assertEquals(applied.subList(0, 256), subset);
        assertEquals(744, budget.omitted(applied.size()));
        assertEquals(0, budget.omitted(12));
    }

    @Test public void pendingRestorationsBeatOlderCosmeticAppliedPairs() {
        UUID viewer = UUID.randomUUID();
        List<RefreshQueue.Pair> applied = new ArrayList<RefreshQueue.Pair>();
        for (int i = 0; i < 8; i++)
            applied.add(new RefreshQueue.Pair(viewer, new UUID(i + 1, 1)));
        RefreshQueue.Pair unknown = new RefreshQueue.Pair(viewer, UUID.randomUUID());
        ShutdownRestorationBudget budget = new ShutdownRestorationBudget(3);
        List<RefreshQueue.Pair> selected = budget.selectPrioritized(applied,
                java.util.Arrays.asList(unknown, applied.get(7), applied.get(6), applied.get(7)));
        assertEquals(java.util.Arrays.asList(applied.get(7), applied.get(6), applied.get(0)), selected);
        assertEquals(5, budget.omitted(applied.size()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesZeroBudget() { new ShutdownRestorationBudget(0); }

    @Test public void emptyAppliedEntriesProduceNoSends() {
        ShutdownRestorationBudget budget = new ShutdownRestorationBudget(256);
        assertTrue(budget.select(new ArrayList<RefreshQueue.Pair>()).isEmpty());
        assertEquals(0, budget.omitted(0));
    }
}
