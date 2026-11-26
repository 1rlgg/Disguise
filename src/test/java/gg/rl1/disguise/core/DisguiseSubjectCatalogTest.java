package gg.rl1.disguise.core;

import gg.rl1.disguise.api.Disguise;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DisguiseSubjectCatalogTest {
    @Test public void includesGlobalAndOverrideOnlySubjectsWithoutDuplicates() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), global = UUID.randomUUID(), overrideOnly = UUID.randomUUID();
        registry.setDisguise(new Disguise(global, "GlobalAlias"));
        registry.setDisguiseFor(viewer, new Disguise(global, "PrivateName"));
        registry.setDisguiseFor(viewer, new Disguise(overrideOnly, "PrivateName"));
        Set<UUID> ids = registry.subjectIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(global));
        assertTrue(ids.contains(overrideOnly));
        registry.clearPlayer(overrideOnly);
        assertEquals(2, ids.size());
        assertEquals(1, registry.subjectIds().size());
    }

    @Test public void fastEmptyCheckTracksViewerOnlyAndGlobalMutations() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        assertFalse(registry.hasAnySubjects());
        registry.setDisguiseFor(viewer, new Disguise(subject, "HiddenAlias"));
        assertTrue(registry.hasAnySubjects());
        registry.removeDisguiseFor(viewer, subject);
        assertFalse(registry.hasAnySubjects());
        registry.setDisguise(new Disguise(subject, "GlobalAlias"));
        assertTrue(registry.hasAnySubjects());
        registry.clear();
        assertFalse(registry.hasAnySubjects());
    }

    @Test public void viewerFastPathIgnoresOverridesOwnedByOtherViewers() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), otherViewer = UUID.randomUUID(), subject = UUID.randomUUID();
        registry.setDisguiseFor(otherViewer, new Disguise(subject, "PrivateName"));
        assertTrue(registry.hasAnySubjects());
        assertFalse(registry.hasAnySubjectsFor(viewer));
        assertTrue(registry.hasAnySubjectsFor(otherViewer));
        registry.setDisguise(new Disguise(subject, "GlobalAlias"));
        assertTrue(registry.hasAnySubjectsFor(viewer));
    }

    @Test public void reusesSubjectSnapshotUntilMembershipChanges() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID();
        registry.setDisguise(new Disguise(subject, "AliasOne"));
        Set<UUID> first = registry.subjectIds();
        assertSame(first, registry.subjectIds());

        registry.setDisguise(new Disguise(subject, "AliasTwo"));
        assertSame(first, registry.subjectIds());

        registry.setDisguise(new Disguise(UUID.randomUUID(), "AliasThree"));
        assertNotSame(first, registry.subjectIds());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotIsImmutable() {
        new InMemoryDisguiseService().subjectIds().add(UUID.randomUUID());
    }

    @Test public void unrelatedPlayerCleanupDoesNotInvalidateSnapshot() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID();
        registry.setDisguise(new Disguise(subject, "AliasOne"));
        Set<UUID> snapshot = registry.subjectIds();
        registry.clearPlayer(UUID.randomUUID());
        assertSame(snapshot, registry.subjectIds());
        registry.clearPlayer(subject);
        assertTrue(registry.subjectIds().isEmpty());
        assertTrue(snapshot.contains(subject));
    }

    @Test public void effectiveSnapshotsIsolateViewersAndSurviveRegistryChanges() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID(), other = UUID.randomUUID();
        Disguise global = new Disguise(subject, "GlobalAlias");
        Disguise privateAlias = new Disguise(subject, "PrivateAlias");
        registry.setDisguise(global);
        registry.setDisguiseFor(viewer, privateAlias);
        registry.setDisguise(new Disguise(viewer, "SelfAlias"));
        java.util.Map<UUID, Disguise> snapshot = registry.effectiveSnapshot(viewer);
        assertEquals(1, snapshot.size());
        assertSame(privateAlias, snapshot.get(subject));
        assertSame(global, registry.effectiveSnapshot(other).get(subject));
        registry.clear();
        assertSame(privateAlias, snapshot.get(subject));
        assertTrue(registry.effectiveSnapshot(viewer).isEmpty());
        try {
            snapshot.clear();
            fail("Effective snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test public void effectiveCacheInvalidatesOnlyAffectedViewerForPrivateMutation() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        registry.setDisguise(new Disguise(subject, "GlobalAlias"));
        java.util.Map<UUID, Disguise> first = registry.effectiveSnapshot(a);
        java.util.Map<UUID, Disguise> second = registry.effectiveSnapshot(b);
        assertSame(first, registry.effectiveSnapshot(a));
        registry.setDisguiseFor(a, new Disguise(subject, "PrivateAlias"));
        assertNotSame(first, registry.effectiveSnapshot(a));
        assertSame(second, registry.effectiveSnapshot(b));
        registry.removeDisguiseFor(a, subject);
        assertEquals("GlobalAlias", registry.effectiveSnapshot(a).get(subject).getName());
        registry.setDisguise(new Disguise(subject, "NewAlias"));
        assertNotSame(second, registry.effectiveSnapshot(b));
    }

    @Test public void snapshotCacheEvictsLeastRecentlyUsedViewer() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = new UUID(1, 0);
        registry.setDisguise(new Disguise(subject, "GlobalAlias"));
        UUID first = new UUID(2, 0), second = new UUID(2, 1);
        java.util.Map<UUID, Disguise> retained = registry.effectiveSnapshot(first);
        java.util.Map<UUID, Disguise> evicted = registry.effectiveSnapshot(second);
        for (int i = 2; i < 256; i++) registry.effectiveSnapshot(new UUID(2, i));
        assertSame(retained, registry.effectiveSnapshot(first));
        registry.effectiveSnapshot(new UUID(2, 256));
        assertSame(retained, registry.effectiveSnapshot(first));
        assertNotSame(evicted, registry.effectiveSnapshot(second));
    }
}
