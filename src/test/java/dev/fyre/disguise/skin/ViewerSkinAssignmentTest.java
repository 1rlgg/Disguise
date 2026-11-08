package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.SkinProperty;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerSkinAssignmentTest {
    private final UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID(), other = UUID.randomUUID();
    private final SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");

    @Test public void onlyUpdatesSelectedViewerOverride() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        Disguise global = new Disguise(subject, "GlobalAlias");
        Disguise custom = new Disguise(subject, "ViewerAlias");
        registry.setDisguise(global);
        registry.setDisguiseFor(viewer, custom);
        assertTrue(new SkinAssignment(registry).applyForViewerIfCurrent(viewer, subject, custom, skin));
        assertSame(skin, registry.getDisguiseFor(viewer, subject).get().getSkin().get());
        assertSame(global, registry.getDisguiseFor(other, subject).get());
    }

    @Test public void neverCreatesOverrideFromGlobalFallback() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        Disguise global = new Disguise(subject, "GlobalAlias");
        registry.setDisguise(global);
        assertFalse(new SkinAssignment(registry).applyForViewerIfCurrent(viewer, subject, global, skin));
        assertSame(global, registry.getDisguiseFor(viewer, subject).get());
        registry.removeDisguise(subject);
        assertFalse(registry.getDisguiseFor(viewer, subject).isPresent());
    }

    @Test public void discardsStaleViewerAlias() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        Disguise old = new Disguise(subject, "OldAlias");
        registry.setDisguiseFor(viewer, old);
        Disguise replacement = new Disguise(subject, "NewAlias");
        registry.setDisguiseFor(viewer, replacement);
        assertFalse(new SkinAssignment(registry).applyForViewerIfCurrent(viewer, subject, old, skin));
        assertSame(replacement, registry.getDisguiseFor(viewer, subject).get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesUnsignedViewerSkin() {
        new SkinAssignment(new InMemoryDisguiseService()).applyForViewerIfCurrent(viewer, subject,
                new Disguise(subject, "AliasName"), new SkinProperty("dGVzdA==", null));
    }
}
