package gg.rl1.disguise.render;

import gg.rl1.disguise.render.restoration.AppliedTabEntries;

import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppliedTabEntriesTest {
    @Test public void containsReportsAppliedPairsAndRestorations() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        assertFalse(entries.contains(viewer, subject));
        entries.applied(viewer, subject);
        assertTrue(entries.contains(viewer, subject));
        entries.restored(viewer, subject);
        assertFalse(entries.contains(viewer, subject));
    }

    @Test public void recordsOnlyDistinctPairsAndRemovesSpecificRestore() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID(), other = UUID.randomUUID();
        entries.applied(viewer, subject);
        entries.applied(viewer, subject);
        entries.applied(viewer, other);
        assertEquals(2, entries.size());
        Set<RefreshQueue.Pair> snapshot = entries.snapshot();
        entries.restored(viewer, subject);
        assertEquals(1, entries.size());
        assertEquals(2, snapshot.size());
        assertTrue(entries.snapshot().contains(new RefreshQueue.Pair(viewer, other)));
    }

    @Test public void reconciliationSnapshotPreservesAppliedInsertionOrder() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        entries.applied(viewer, a);
        entries.applied(viewer, b);
        entries.applied(viewer, c);
        java.util.List<RefreshQueue.Pair> ordered = new java.util.ArrayList<RefreshQueue.Pair>(entries.snapshot());
        assertEquals(a, ordered.get(0).getSubject());
        assertEquals(b, ordered.get(1).getSubject());
        assertEquals(c, ordered.get(2).getSubject());
        entries.restored(viewer, b);
        entries.applied(viewer, b);
        ordered = new java.util.ArrayList<RefreshQueue.Pair>(entries.snapshot());
        assertEquals(b, ordered.get(2).getSubject());
    }

    @Test public void snapshotsAreCachedUntilMutationAndRemainImmutable() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID viewer = UUID.randomUUID(), first = UUID.randomUUID(), next = UUID.randomUUID();
        entries.applied(viewer, first);
        Set<RefreshQueue.Pair> firstSnapshot = entries.snapshot();
        assertSame(firstSnapshot, entries.snapshot());
        entries.applied(viewer, first);
        assertSame(firstSnapshot, entries.snapshot());
        entries.applied(viewer, next);
        assertNotSame(firstSnapshot, entries.snapshot());
        assertEquals(1, firstSnapshot.size());
        assertEquals(2, entries.snapshot().size());
        try {
            firstSnapshot.clear();
            fail("snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {

        }
        entries.forget(viewer);
        assertTrue(entries.snapshot().isEmpty());
    }

    @Test public void forgetClearsBothViewerAndSubjectReferences() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID id = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        entries.applied(id, a);
        entries.applied(a, id);
        entries.applied(a, b);
        entries.forget(id);
        assertEquals(1, entries.size());
        entries.clear();
        assertEquals(0, entries.size());
    }

    @Test public void indexedSnapshotsRemainStableAcrossMutations() {
        AppliedTabEntries entries = new AppliedTabEntries();
        UUID viewer = UUID.randomUUID();
        for (int i = 0; i < 300; i++) entries.applied(viewer, new UUID(0, i));
        java.util.List<RefreshQueue.Pair> snapshot = entries.scanSnapshot();
        assertSame(snapshot, entries.scanSnapshot());
        assertTrue(snapshot instanceof java.util.RandomAccess);
        gg.rl1.disguise.render.restoration.RestorationReconciler reconciler =
                new gg.rl1.disguise.render.restoration.RestorationReconciler();
        for (int i = 0; i < 300; i++) {
            assertEquals(snapshot.get(i), reconciler.scan(snapshot, pair -> true, 1).get(0));
        }
        assertEquals(snapshot.get(0), reconciler.scan(snapshot, pair -> true, 1).get(0));
        entries.clear();
        assertEquals(300, snapshot.size());
        assertTrue(entries.scanSnapshot().isEmpty());
    }
}
