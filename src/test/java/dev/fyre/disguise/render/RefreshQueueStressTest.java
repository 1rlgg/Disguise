package dev.fyre.disguise.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Deterministic stress coverage for PvP-scale alias churn: preserve bounds,
 * avoid duplicated pairs and prevent cosmetic work overtaking restoration.
 */
public class RefreshQueueStressTest {
    @Test public void survivesTenThousandMixedMutationsWithoutDuplicateWork() {
        final int capacity = 64;
        RefreshQueue queue = new RefreshQueue(capacity);
        Random random = new Random(971892L);
        List<UUID> players = new ArrayList<UUID>();
        for (int i = 0; i < 32; i++) players.add(new UUID(i + 1L, ~((long) i)));
        for (int i = 0; i < 10000; i++) {
            UUID viewer = players.get(random.nextInt(players.size()));
            UUID subject = players.get(random.nextInt(players.size()));
            int mode = random.nextInt(10);
            if (mode < 5) queue.offer(viewer, subject);
            else if (mode < 8) queue.offerRestoration(viewer, subject);
            else if (mode == 8) queue.forget(viewer);
            else {
                List<RefreshQueue.Pair> done = queue.poll(1 + random.nextInt(12));
                Set<RefreshQueue.Pair> seen = new HashSet<RefreshQueue.Pair>(done);
                assertEquals("Duplicate pending work", seen.size(), done.size());
            }
            assertTrue("Bound exceeded after iteration " + i, queue.size() <= capacity);
            assertTrue(queue.pendingRestorations() <= queue.size());
            assertTrue(queue.droppedCosmetic() >= 0);
            assertTrue(queue.droppedRestoration() >= 0);
        }
        List<RefreshQueue.Pair> drained = queue.poll(capacity);
        assertEquals(new HashSet<RefreshQueue.Pair>(drained).size(), drained.size());
        assertEquals(0, queue.size());
        assertEquals(0, queue.pendingRestorations());
    }

    @Test public void noCosmeticWorkRunsAheadOfPendingRestoration() {
        RefreshQueue queue = new RefreshQueue(20);
        UUID viewer = UUID.randomUUID();
        for (int i = 0; i < 10; i++) queue.offer(viewer, new UUID(i + 1, 1));
        for (int i = 0; i < 5; i++) queue.offerRestoration(viewer, new UUID(i + 100, 1));
        assertEquals(5, queue.pendingRestorations());
        List<RefreshQueue.Pair> first = queue.poll(5);
        for (RefreshQueue.Pair pair : first) {
            assertTrue(pair.getSubject().getMostSignificantBits() >= 100);
        }
        assertEquals(0, queue.pendingRestorations());
        assertEquals(10, queue.size());
    }
}
