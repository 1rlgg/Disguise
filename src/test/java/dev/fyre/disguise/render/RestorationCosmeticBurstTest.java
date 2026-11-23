package dev.fyre.disguise.render;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RestorationCosmeticBurstTest {
    @Test public void urgentBurstPreservesDisplacedAliasUpdatesWithoutDuplicateSends() {
        UUID viewer = UUID.randomUUID();
        RefreshQueue primary = new RefreshQueue(8);
        DeferredRefreshBacklog deferred = new DeferredRefreshBacklog(16);
        Set<UUID> expected = new HashSet<UUID>();
        for (int i = 0; i < 8; i++) {
            UUID subject = new UUID(i + 1, 1);
            expected.add(subject);
            assertTrue(primary.offer(viewer, subject));
        }
        for (int i = 0; i < 8; i++) {
            UUID subject = new UUID(i + 100, 1);
            expected.add(subject);
            assertTrue(primary.offerRestoration(viewer, subject, pair ->
                    deferred.offer(pair.getViewer(), pair.getSubject())));
        }
        assertEquals(8, primary.pendingRestorations());
        assertEquals(8, deferred.size());
        assertEquals(8, primary.droppedCosmetic());
        Set<UUID> delivered = new HashSet<UUID>();
        for (RefreshQueue.Pair pair : primary.poll(8)) {
            assertTrue("Duplicate pair delivered", delivered.add(pair.getSubject()));
            assertTrue(pair.getSubject().getMostSignificantBits() >= 100);
        }
        assertEquals(8, deferred.drainTo(primary, 8));
        for (RefreshQueue.Pair pair : primary.poll(8)) {
            assertTrue("Duplicate pair delivered", delivered.add(pair.getSubject()));
        }
        assertEquals(expected, delivered);
        assertEquals(0, primary.size());
        assertEquals(0, deferred.size());
    }

    @Test public void shutdownPrioritizationPrefersUrgentWorkAndRespectsCap() {
        UUID viewer = UUID.randomUUID();
        AppliedTabEntries applied = new AppliedTabEntries();
        RefreshQueue queue = new RefreshQueue(32);
        for (int i = 0; i < 24; i++) {
            UUID subject = new UUID(i + 1, 1);
            applied.applied(viewer, subject);
            if (i >= 20) queue.offerRestoration(viewer, subject);
        }
        ShutdownRestorationBudget budget = new ShutdownRestorationBudget(8);
        java.util.List<RefreshQueue.Pair> chosen = budget.selectPrioritized(
                applied.snapshot(), queue.pendingRestorationSnapshot());
        assertEquals(8, chosen.size());
        for (int i = 0; i < 4; i++)
            assertEquals(21 + i, chosen.get(i).getSubject().getMostSignificantBits());
        assertEquals(16, budget.omitted(applied.size()));
    }
}
