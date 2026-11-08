package dev.fyre.disguise.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RestorationReconcilerTest {
    private RefreshQueue.Pair pair() {
        return new RefreshQueue.Pair(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test public void scansAllEntriesAcrossMultipleBoundedCalls() {
        RestorationReconciler scanner = new RestorationReconciler();
        List<RefreshQueue.Pair> entries = Arrays.asList(pair(), pair(), pair(), pair(), pair());
        assertEquals(entries.subList(0, 2), scanner.scan(entries, p -> true, 2));
        assertEquals(entries.subList(2, 4), scanner.scan(entries, p -> true, 2));
        assertEquals(Arrays.asList(entries.get(4), entries.get(0)),
                scanner.scan(entries, p -> true, 2));
    }

    @Test public void onlySelectsAppliedPairsThatNoLongerHaveDisguises() {
        RestorationReconciler scanner = new RestorationReconciler();
        RefreshQueue.Pair active = pair(), undisguised = pair();
        assertEquals(Collections.singletonList(undisguised),
                scanner.scan(Arrays.asList(active, undisguised), undisguised::equals, 10));
    }

    @Test public void wrapsWithoutMakingAnotherFullCollectionCopy() {
        RestorationReconciler scanner = new RestorationReconciler();
        List<RefreshQueue.Pair> entries = Arrays.asList(pair(), pair(), pair());
        assertEquals(entries.subList(0, 2), scanner.scan(entries, p -> true, 2));
        assertEquals(Arrays.asList(entries.get(2), entries.get(0)),
                scanner.scan(entries, p -> true, 2));
        assertEquals(Arrays.asList(entries.get(1), entries.get(2)),
                scanner.scan(entries, p -> true, 2));
        scanner.reset();
        assertEquals(entries.subList(0, 2), scanner.scan(entries, p -> true, 2));
    }

    @Test public void emptyRegistryAndResetAreSafe() {
        RestorationReconciler scanner = new RestorationReconciler();
        assertTrue(scanner.scan(Collections.emptyList(), pair -> true, 5).isEmpty());
        scanner.reset();
        assertTrue(scanner.scan(Collections.emptyList(), pair -> true, 5).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectUnboundedInvalidScan() {
        new RestorationReconciler().scan(Collections.emptyList(), pair -> true, 0);
    }
}
