package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.render.IdentityTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClearViewerRestorationTest {
    @Test public void clearPlayerRestoresOtherSubjectsGlobalAliasForClearedViewer() {
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID(), other = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry,
                events::add, () -> Arrays.asList(viewer, other));
        service.setDisguise(new Disguise(subject, "GlobalAlias"));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerAlias"));
        events.clear();

        service.clearPlayer(viewer);

        assertEquals("GlobalAlias", registry.getDisguiseFor(viewer, subject).get().getName());
        assertEquals(1, events.size());
        assertEquals(viewer, events.get(0).getViewerId());
        assertEquals(subject, events.get(0).getSubjectId());
        assertEquals("ViewerAlias", events.get(0).getBefore().get().getName());
        assertEquals("GlobalAlias", events.get(0).getAfter().get().getName());
    }

    @Test public void clearPlayerRemovesOverrideOnlySubjectsForThatViewer() {
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry,
                events::add, () -> Arrays.asList(viewer));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerAlias"));
        events.clear();

        service.clearPlayer(viewer);

        assertEquals(1, events.size());
        assertEquals(subject, events.get(0).getSubjectId());
        assertFalse(events.get(0).getAfter().isPresent());
    }

    @Test public void clearPlayerDoesNotNotifyOfflineViewer() {
        UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID(), online = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(registry,
                events::add, () -> Arrays.asList(online));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerAlias"));
        service.clearPlayer(viewer);
        assertTrue(events.isEmpty());
    }
}
