package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.render.identity.IdentityTransition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObservableDisguiseServiceTest {
    @Test public void globalChangesRespectViewerOverrides() {
        UUID subject = UUID.randomUUID(), viewerA = UUID.randomUUID(), viewerB = UUID.randomUUID();
        List<IdentityTransition> events = new ArrayList<IdentityTransition>();
        ObservableDisguiseService service = new ObservableDisguiseService(
                new InMemoryDisguiseService(), events::add, () -> Arrays.asList(viewerA, viewerB));
        service.setDisguiseFor(viewerA, new Disguise(subject, "PrivateName"));
        events.clear();
        service.setDisguise(new Disguise(subject, "GlobalName"));
        assertEquals(1, events.size());
        assertEquals(viewerB, events.get(0).getViewerId());
        events.clear();
        service.setDisguise(new Disguise(subject, "GlobalName"));
        assertTrue(events.isEmpty());
        service.removeDisguiseFor(viewerA, subject);
        assertEquals(1, events.size());
        assertEquals("GlobalName", events.get(0).getAfter().get().getName());
    }
}
