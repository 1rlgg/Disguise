package dev.fyre.disguise.render;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshTargetPlannerTest {
    @Test public void doesNotRefreshUnrelatedPlayers() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID joining = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(new RefreshTargetPlanner(registry).around(joining, Arrays.asList(joining, a, b)).isEmpty());
    }

    @Test public void includesBothDirectionsOnlyWhenDisguised() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID joining = UUID.randomUUID(), other = UUID.randomUUID(), irrelevant = UUID.randomUUID();
        registry.setDisguise(new Disguise(other, "AliasOne"));
        registry.setDisguiseFor(other, new Disguise(joining, "AliasTwo"));
        Collection<RefreshQueue.Pair> planned = new RefreshTargetPlanner(registry)
                .around(joining, Arrays.asList(joining, other, irrelevant, other));
        assertEquals(2, planned.size());
        assertTrue(planned.contains(new RefreshQueue.Pair(joining, other)));
        assertTrue(planned.contains(new RefreshQueue.Pair(other, joining)));
    }

    @Test public void fallsBackToGlobalForViewerWithoutOverride() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID joining = UUID.randomUUID(), other = UUID.randomUUID();
        registry.setDisguise(new Disguise(other, "GlobalAlias"));
        assertTrue(new RefreshTargetPlanner(registry).around(joining, Arrays.asList(other))
                .contains(new RefreshQueue.Pair(joining, other)));
    }
}
