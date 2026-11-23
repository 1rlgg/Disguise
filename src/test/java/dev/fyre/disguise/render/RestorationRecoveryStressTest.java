package dev.fyre.disguise.render;

import dev.fyre.disguise.render.restoration.AppliedTabEntries;
import dev.fyre.disguise.render.restoration.RestorationReconciler;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RestorationRecoveryStressTest {
    @Test public void eventuallySchedulesEveryDisplacedRestorationWithoutEviction() {
        UUID viewer = UUID.randomUUID();
        AppliedTabEntries applied = new AppliedTabEntries();
        RefreshQueue pending = new RefreshQueue(8);
        RestorationReconciler reconciler = new RestorationReconciler();
        for (int i = 0; i < 96; i++) {
            UUID subject = new UUID(i + 1L, i + 700L);
            applied.applied(viewer, subject);
            pending.offerRestoration(viewer, subject);
        }
        assertEquals(8, pending.pendingRestorations());
        assertTrue(pending.droppedRestoration() > 0);
        int restored = 0;
        for (int tick = 0; tick < 100 && applied.size() > 0; tick++) {
            for (RefreshQueue.Pair pair : reconciler.scan(applied.snapshot(), p -> true, 16)) {
                pending.offerRestoration(pair.getViewer(), pair.getSubject());
            }
            for (RefreshQueue.Pair pair : pending.poll(8)) {
                assertTrue("Only applied entries can be restored",
                        applied.contains(pair.getViewer(), pair.getSubject()));
                applied.restored(pair.getViewer(), pair.getSubject());
                restored++;
            }
            assertTrue(pending.size() <= 8);
        }
        assertEquals(96, restored);
        assertEquals(0, applied.size());
        assertEquals(0, pending.size());
    }

    @Test public void cosmeticTrafficCannotDisplaceExistingRestorations() {
        UUID viewer = UUID.randomUUID();
        RefreshQueue pending = new RefreshQueue(3);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        pending.offerRestoration(viewer, a);
        pending.offerRestoration(viewer, b);
        pending.offerRestoration(viewer, c);
        for (int i = 0; i < 1000; i++) {
            assertFalse(pending.offer(viewer, new UUID(i + 100L, i + 200L)));
        }
        assertEquals(a, pending.poll(1).get(0).getSubject());
        assertEquals(b, pending.poll(1).get(0).getSubject());
        assertEquals(c, pending.poll(1).get(0).getSubject());
    }
}
