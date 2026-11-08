package dev.fyre.disguise.render;

import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppliedTabEntriesTest {
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
}
