package dev.fyre.disguise.render;

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
}
