package gg.rl1.disguise.core;

import gg.rl1.disguise.api.Disguise;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class InMemoryDisguiseServiceTest {
    private final UUID subject = UUID.randomUUID();
    private final UUID viewer = UUID.randomUUID();

    @Test public void overrideTakesPriorityAndFallsBackToGlobal() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        service.setDisguise(new Disguise(subject, "GlobalName"));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerName"));
        assertEquals("ViewerName", service.getDisguiseFor(viewer, subject).get().getName());
        assertTrue(service.removeDisguiseFor(viewer, subject));
        assertEquals("GlobalName", service.getDisguiseFor(viewer, subject).get().getName());
        assertFalse(service.removeDisguiseFor(viewer, subject));
    }

    @Test public void quitCleanupRemovesSubjectAndViewerReferences() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        service.setDisguise(new Disguise(subject, "GlobalName"));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerName"));
        service.setDisguiseFor(subject, new Disguise(viewer, "ThirdName"));
        service.clearPlayer(subject);
        assertFalse(service.getDisguise(subject).isPresent());
        assertFalse(service.getDisguiseFor(viewer, subject).isPresent());
        assertFalse(service.getDisguiseFor(subject, viewer).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidNameIsRejected() {
        new Disguise(subject, "Invalid Name");
    }

    @Test public void clearingRegistryRemovesOverrides() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        service.setDisguiseFor(viewer, new Disguise(subject, "SomeName"));
        service.clear();
        assertFalse(service.getDisguiseFor(viewer, subject).isPresent());
    }
}
