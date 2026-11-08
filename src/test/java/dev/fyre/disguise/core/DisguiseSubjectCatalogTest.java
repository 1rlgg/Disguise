package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
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

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotIsImmutable() {
        new InMemoryDisguiseService().subjectIds().add(UUID.randomUUID());
    }
}
