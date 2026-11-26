package gg.rl1.disguise.core;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.render.identity.IdentityTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClearRestorationTest {
    @Test public void clearPlayerRestoresGlobalAndOverrideViewers() {
        UUID subject = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry, events::add,
                () -> Arrays.asList(first, second));
        service.setDisguise(new Disguise(subject, "GlobalAlias"));
        service.setDisguiseFor(first, new Disguise(subject, "PrivateAlias"));
        events.clear();
        service.clearPlayer(subject);
        assertEquals(2, events.size());
        for (IdentityTransition change : events) {
            assertEquals(subject, change.getSubjectId());
            assertTrue(change.getBefore().isPresent());
            assertFalse(change.getAfter().isPresent());
        }
        assertFalse(registry.getDisguiseFor(first, subject).isPresent());
        assertFalse(registry.getDisguiseFor(second, subject).isPresent());
    }

    @Test public void clearRestoresOverrideOnlySubjectsAndGlobalSubjects() {
        UUID global = UUID.randomUUID(), privateOnly = UUID.randomUUID();
        UUID viewer = UUID.randomUUID(), second = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry, events::add,
                () -> Arrays.asList(viewer, second));
        service.setDisguise(new Disguise(global, "GlobalAlias"));
        service.setDisguiseFor(viewer, new Disguise(privateOnly, "ViewerAlias"));
        events.clear();
        service.clear();
        assertEquals(3, events.size());
        for (IdentityTransition change : events) {
            assertFalse(change.getAfter().isPresent());
        }
        assertTrue(registry.subjectIds().isEmpty());
        assertTrue(events.stream().anyMatch(e -> e.getSubjectId().equals(privateOnly)
                && e.getViewerId().equals(viewer)));
    }

    @Test public void emergencyClearRestoresEachOnlineViewerExactlyOnce() {
        UUID target = UUID.randomUUID(), viewer1 = UUID.randomUUID(), viewer2 = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> updates = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry, updates::add,
                () -> Arrays.asList(viewer1, viewer2));
        service.setDisguise(new Disguise(target, "TestGlobal"));
        service.setDisguiseFor(viewer1, new Disguise(target, "TestPrivate"));
        updates.clear();
        service.clear();
        assertEquals(2, updates.size());
        assertTrue(updates.stream().allMatch(e -> !e.getAfter().isPresent()));
        assertEquals(2, updates.stream().map(IdentityTransition::getViewerId).distinct().count());
        service.clear();
        assertEquals("Reset should be idempotent", 2, updates.size());
    }

    @Test public void emptyRegistryDoesNotEmitChanges() {
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), events::add, Collections::emptyList);
        service.clear();
        service.clearPlayer(UUID.randomUUID());
        assertTrue(events.isEmpty());
    }
}
