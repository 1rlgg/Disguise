package dev.fyre.disguise.protocol;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnIdentityDecisionTest {
    @Test public void selectsViewerOverrideWithoutAlteringVisibility() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID(), other = UUID.randomUUID();
        service.setDisguise(new Disguise(subject, "GlobalAlias"));
        service.setDisguiseFor(viewer, new Disguise(subject, "ViewerAlias"));
        SpawnIdentityDecision decision = new SpawnIdentityDecision(service);
        assertEquals("ViewerAlias", decision.desiredIdentity(viewer, subject, true).get().getName());
        assertEquals("GlobalAlias", decision.desiredIdentity(other, subject, true).get().getName());
        assertFalse(decision.desiredIdentity(viewer, subject, false).isPresent());
        assertFalse(decision.desiredIdentity(subject, subject, true).isPresent());
    }

    @Test public void leavesUndisguisedSpawnAlone() {
        assertFalse(new SpawnIdentityDecision(new InMemoryDisguiseService())
                .desiredIdentity(UUID.randomUUID(), UUID.randomUUID(), true).isPresent());
    }
}
