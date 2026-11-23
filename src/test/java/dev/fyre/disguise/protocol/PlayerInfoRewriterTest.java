package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.packet.PlayerInfoRewriter;

import dev.fyre.disguise.protocol.entity.EntityRefreshPolicy;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoRewriterTest {
    @Test public void preservesEmptyPacketDataWithoutServerRuntime() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        PlayerInfoRewriter rewriter = new PlayerInfoRewriter(registry);
        assertSame(Collections.emptyList(), rewriter.rewrite(UUID.randomUUID(), Collections.emptyList()));
    }

    @Test public void desiredAliasCollisionIsDetectableOffThreadWithoutBukkit() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        registry.setDisguise(new Disguise(a, "Duplicate"));
        registry.setDisguise(new Disguise(b, "duplicate"));
        assertTrue(EntityRefreshPolicy.conflicts(registry, viewer, a,
                "Duplicate", registry.subjectIds()));
        registry.setDisguiseFor(viewer, new Disguise(b, "Different"));
        assertFalse(EntityRefreshPolicy.conflicts(registry, viewer, a,
                "Duplicate", registry.subjectIds()));
    }

    @Test public void viewerOverrideAndRemovalResolveWithoutServerRuntime() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        registry.setDisguise(new Disguise(subject, "GlobalAlias"));
        registry.setDisguiseFor(viewer, new Disguise(subject, "ViewerAlias"));
        assertEquals("ViewerAlias", registry.getDisguiseFor(viewer, subject).get().getName());
        registry.removeDisguiseFor(viewer, subject);
        assertEquals("GlobalAlias", registry.getDisguiseFor(viewer, subject).get().getName());
        registry.removeDisguise(subject);
        assertFalse(registry.getDisguiseFor(viewer, subject).isPresent());
    }
}
