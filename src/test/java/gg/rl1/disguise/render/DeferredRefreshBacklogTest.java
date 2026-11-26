package gg.rl1.disguise.render;

import gg.rl1.disguise.render.refresh.DeferredRefreshBacklog;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeferredRefreshBacklogTest {
    @Test public void replaysRejectedCosmeticsAfterUrgentRestorationDrains() {
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        RefreshQueue primary = new RefreshQueue(1);
        DeferredRefreshBacklog deferred = new DeferredRefreshBacklog(2);
        primary.offerRestoration(viewer, a);
        assertTrue(deferred.offer(viewer, b));
        assertEquals(0, deferred.drainTo(primary, 2));
        assertEquals(a, primary.poll(1).get(0).getSubject());
        assertEquals(1, deferred.drainTo(primary, 2));
        assertEquals(b, primary.poll(1).get(0).getSubject());
        assertEquals(0, deferred.size());
    }

    @Test public void duplicatePrimaryWorkPrunesDeferredEntryEvenWhenPrimaryFull() {
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        RefreshQueue primary = new RefreshQueue(1);
        DeferredRefreshBacklog deferred = new DeferredRefreshBacklog(2);
        deferred.offer(viewer, subject);
        primary.offerRestoration(viewer, subject);
        assertEquals(0, deferred.drainTo(primary, 1));
        assertEquals(0, deferred.size());
        assertEquals(1, primary.size());
        assertEquals(subject, primary.poll(1).get(0).getSubject());
    }

    @Test public void evictedCosmeticSurvivesRestorationPriorityInDeferredBacklog() {
        UUID viewer = UUID.randomUUID(), first = UUID.randomUUID(),
                second = UUID.randomUUID(), restore = UUID.randomUUID();
        RefreshQueue primary = new RefreshQueue(2);
        DeferredRefreshBacklog backlog = new DeferredRefreshBacklog(2);
        primary.offer(viewer, first);
        primary.offer(viewer, second);
        primary.offerRestoration(viewer, restore, pair ->
                backlog.offer(pair.getViewer(), pair.getSubject()));
        assertEquals(restore, primary.poll(1).get(0).getSubject());
        assertEquals(second, primary.poll(1).get(0).getSubject());
        assertEquals(1, backlog.drainTo(primary, 2));
        assertEquals(first, primary.poll(1).get(0).getSubject());
        assertEquals(0, backlog.size());
    }

    @Test public void urgentRestorationCancelsStaleCosmeticWork() {
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        DeferredRefreshBacklog backlog = new DeferredRefreshBacklog(2);
        backlog.offer(viewer, subject);
        assertTrue(backlog.cancel(viewer, subject));
        assertFalse(backlog.cancel(viewer, subject));
        assertEquals(0, backlog.size());
    }

    @Test public void duplicateOffersAreCoalescedAndOverflowIsCounted() {
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        DeferredRefreshBacklog deferred = new DeferredRefreshBacklog(1);
        assertTrue(deferred.offer(viewer, a));
        assertTrue(deferred.offer(viewer, a));
        assertFalse(deferred.offer(viewer, b));
        assertEquals(1, deferred.size());
        assertEquals(1, deferred.overflowCount());
        deferred.forget(viewer);
        assertEquals(0, deferred.size());
        deferred.clear();
        assertEquals(0, deferred.overflowCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCapacityIsRejected() { new DeferredRefreshBacklog(0); }
    @Test public void duplicateInspectionIsBounded() {
        UUID viewer = UUID.randomUUID();
        RefreshQueue primary = new RefreshQueue(8);
        DeferredRefreshBacklog backlog = new DeferredRefreshBacklog(8);
        for (int i = 0; i < 8; i++) {
            RefreshQueue.Pair pair = new RefreshQueue.Pair(viewer, new UUID(0, i));
            primary.offer(pair);
            backlog.offer(pair);
        }
        assertEquals(0, backlog.drainTo(primary, 3));
        assertEquals(5, backlog.size());
    }

    @Test public void promotionRetainsOriginalPairInstance() {
        RefreshQueue primary = new RefreshQueue(1);
        DeferredRefreshBacklog backlog = new DeferredRefreshBacklog(1);
        RefreshQueue.Pair pair = new RefreshQueue.Pair(UUID.randomUUID(), UUID.randomUUID());
        assertTrue(backlog.offer(pair));
        assertEquals(1, backlog.drainTo(primary, 1));
        assertSame(pair, primary.poll(1).get(0));
    }
}
