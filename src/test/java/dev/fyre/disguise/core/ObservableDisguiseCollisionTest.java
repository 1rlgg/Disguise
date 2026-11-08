package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.render.IdentityTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests a collision appearing and later disappearing without changing B's own alias. */
public class ObservableDisguiseCollisionTest {
    @Test public void newCollisionTriggersRefreshForPreviouslyUnchangedAlias() {
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), events::add, () -> Arrays.asList(viewer));
        service.setDisguise(new Disguise(b, "SharedAlias"));
        events.clear();
        service.setDisguise(new Disguise(a, "sharedalias"));
        assertEquals(2, events.size());
        assertEquals(a, events.get(0).getSubjectId());
        assertEquals(b, events.get(1).getSubjectId());
        assertEquals("SharedAlias", events.get(1).getAfter().get().getName());
        events.clear();
        service.removeDisguise(a);
        assertEquals(2, events.size());
        assertEquals(b, events.get(1).getSubjectId());
    }

    @Test public void collisionRefreshIsOnlyForAffectedViewerAndAlias() {
        UUID viewerA = UUID.randomUUID(), viewerB = UUID.randomUUID();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), events::add, () -> Arrays.asList(viewerA, viewerB));
        service.setDisguise(new Disguise(b, "SharedAlias"));
        service.setDisguise(new Disguise(c, "Unrelated"));
        events.clear();
        service.setDisguiseFor(viewerA, new Disguise(a, "SharedAlias"));
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getViewerId().equals(viewerA)));
        assertTrue(events.stream().noneMatch(e -> e.getSubjectId().equals(c)));
        events.clear();
        service.setDisguiseFor(viewerA, new Disguise(a, "Different"));
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getViewerId().equals(viewerA)));
    }

    @Test public void skinChangeWithoutAliasNameChangeDoesNotRefreshNeighbours() {
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), events::add, () -> Arrays.asList(viewer));
        service.setDisguise(new Disguise(a, "SameAlias"));
        service.setDisguise(new Disguise(b, "SameAlias"));
        events.clear();
        service.setDisguise(new Disguise(a, "SameAlias"));
        assertTrue(events.isEmpty());
    }
}
