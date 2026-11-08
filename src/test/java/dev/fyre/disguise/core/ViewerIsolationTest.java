package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.render.IdentityTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerIsolationTest {
    @Test public void clearingViewerDoesNotRemoveGlobalDisguiseForOthers() {
        UUID subject = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        InMemoryDisguiseService state = new InMemoryDisguiseService();
        state.setDisguise(new Disguise(subject, "MainAlias"));
        state.setDisguiseFor(first, new Disguise(subject, "OnlyFirst"));
        state.clearPlayer(first);
        assertEquals("MainAlias", state.getDisguiseFor(second, subject).get().getName());
        assertEquals("MainAlias", state.getDisguiseFor(first, subject).get().getName());
    }

    @Test public void perViewerMutationsOnlyNotifyTargetOnlineViewer() {
        UUID subject = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        List<IdentityTransition> transitions = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), transitions::add, () -> Arrays.asList(first, second));
        service.setDisguiseFor(first, new Disguise(subject, "OnlyFirst"));
        assertEquals(1, transitions.size());
        assertEquals(first, transitions.get(0).getViewerId());
        service.removeDisguiseFor(first, subject);
        assertEquals(2, transitions.size());
        assertFalse(transitions.get(1).getAfter().isPresent());
    }
}
