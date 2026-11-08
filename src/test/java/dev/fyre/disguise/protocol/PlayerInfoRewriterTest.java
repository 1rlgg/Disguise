package dev.fyre.disguise.protocol;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Only pure behavior can be tested without a running Minecraft server.
 * ProtocolLib GameProfile/PlayerInfoData initialization requires server internals,
 * so actual packet serialization must be verified in an integration test.
 */
public class PlayerInfoRewriterTest {
    @Test public void preservesEmptyPacketDataWithoutServerRuntime() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        PlayerInfoRewriter rewriter = new PlayerInfoRewriter(registry);
        assertSame(Collections.emptyList(), rewriter.rewrite(UUID.randomUUID(), Collections.emptyList()));
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
