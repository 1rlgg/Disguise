package dev.fyre.disguise.render;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshQueueTest {
    @Test public void coalescesAndKeepsInsertionOrder() {
        RefreshQueue queue = new RefreshQueue(2);
        UUID viewer = UUID.randomUUID(), one = UUID.randomUUID(), two = UUID.randomUUID(), three = UUID.randomUUID();
        assertTrue(queue.offer(viewer, one));
        assertTrue(queue.offer(viewer, one));
        assertTrue(queue.offer(viewer, two));
        assertFalse(queue.offer(viewer, three));
        assertEquals(2, queue.size());
        List<RefreshQueue.Pair> first = queue.poll(1);
        assertEquals(one, first.get(0).getSubject());
        assertTrue(queue.offer(viewer, three));
        assertEquals(two, queue.poll(1).get(0).getSubject());
        assertEquals(three, queue.poll(1).get(0).getSubject());
        assertEquals(0, queue.size());
    }

    @Test public void discardsAllQueuedWorkForDisconnectedPlayer() {
        RefreshQueue queue = new RefreshQueue(5);
        UUID one = UUID.randomUUID(), two = UUID.randomUUID(), three = UUID.randomUUID();
        queue.offer(one, two);
        queue.offer(three, one);
        queue.offer(two, three);
        queue.forget(one);
        assertEquals(1, queue.size());
        assertEquals(three, queue.poll(3).get(0).getSubject());
    }

    @Test public void skipsSelfAndClears() {
        RefreshQueue queue = new RefreshQueue(1);
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        assertTrue(queue.offer(one, one));
        assertEquals(0, queue.size());
        assertTrue(queue.offer(one, two));
        queue.clear();
        assertEquals(0, queue.size());
    }

    @Test public void restorationReplacesOldestPendingCosmeticWhenFull() {
        RefreshQueue queue = new RefreshQueue(2);
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(),
                b = UUID.randomUUID(), restored = UUID.randomUUID();
        assertTrue(queue.offer(viewer, a));
        assertTrue(queue.offer(viewer, b));
        assertTrue(queue.offerRestoration(viewer, restored));
        assertEquals(2, queue.size());
        assertEquals(restored, queue.poll(1).get(0).getSubject());
        assertEquals(a, queue.poll(1).get(0).getSubject());
        assertEquals(0, queue.size());
    }

    @Test public void restoringAlreadyQueuedPairDoesNotDuplicate() {
        RefreshQueue queue = new RefreshQueue(1);
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        assertTrue(queue.offer(viewer, subject));
        assertTrue(queue.offerRestoration(viewer, subject));
        assertEquals(1, queue.poll(10).size());
    }

    @Test public void restorationPromotesAlreadyQueuedPairAheadOfCosmeticWork() {
        RefreshQueue queue = new RefreshQueue(3);
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        queue.offer(viewer, a);
        queue.offer(viewer, b);
        queue.offerRestoration(viewer, b);
        assertEquals(2, queue.size());
        assertEquals(b, queue.poll(1).get(0).getSubject());
        assertEquals(a, queue.poll(1).get(0).getSubject());
    }

    @Test public void restoredEntriesSurviveWhenCosmeticQueueOverflows() {
        RefreshQueue queue = new RefreshQueue(2);
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        queue.offerRestoration(viewer, a);
        queue.offer(viewer, b);
        queue.offerRestoration(viewer, c);
        assertEquals(a, queue.poll(1).get(0).getSubject());
        assertEquals(c, queue.poll(1).get(0).getSubject());
    }

    @Test public void reportsDropsAndKeepsPendingRestorationSetConsistent() {
        RefreshQueue queue = new RefreshQueue(2);
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(),
                b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        queue.offerRestoration(viewer, a);
        queue.offerRestoration(viewer, b);
        assertFalse(queue.offer(viewer, c));
        assertEquals(1, queue.droppedCosmetic());
        assertFalse(queue.offerRestoration(viewer, d));
        assertEquals(1, queue.droppedRestoration());
        assertEquals(2, queue.pendingRestorations());
        assertEquals(a, queue.poll(1).get(0).getSubject());
        assertEquals(1, queue.pendingRestorations());
        queue.forget(viewer);
        assertEquals(0, queue.pendingRestorations());
        assertEquals(0, queue.size());
        queue.clear();
        assertEquals(0, queue.droppedCosmetic());
        assertEquals(0, queue.droppedRestoration());
    }

    @Test public void fullRestorationQueueNeverDisplacesOldestPendingRestore() {
        RefreshQueue queue = new RefreshQueue(2);
        UUID viewer = UUID.randomUUID(), first = UUID.randomUUID(),
                second = UUID.randomUUID(), rejected = UUID.randomUUID();
        assertTrue(queue.offerRestoration(viewer, first));
        assertTrue(queue.offerRestoration(viewer, second));
        assertFalse(queue.offerRestoration(viewer, rejected));
        assertEquals(first, queue.poll(1).get(0).getSubject());
        assertEquals(second, queue.poll(1).get(0).getSubject());
        assertEquals(1, queue.droppedRestoration());
        assertTrue(queue.offerRestoration(viewer, rejected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCapacity() { new RefreshQueue(0); }
}
