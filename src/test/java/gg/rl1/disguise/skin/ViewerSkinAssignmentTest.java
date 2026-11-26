package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.SkinProperty;
import gg.rl1.disguise.core.InMemoryDisguiseService;
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

    @Test public void allowsExplicitOverrideSharingSameImmutableGlobalDisguiseInstance() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        Disguise shared = new Disguise(subject, "SharedAlias");
        registry.setDisguise(shared);
        registry.setDisguiseFor(viewer, shared);
        assertSame(shared, registry.getViewerOverride(viewer, subject).get());
        assertTrue(new SkinAssignment(registry).applyForViewerIfCurrent(viewer, subject, shared, skin));
        assertSame(shared, registry.getDisguise(subject).get());
        assertFalse(registry.getDisguise(subject).get().getSkin().isPresent());
        assertSame(skin, registry.getViewerOverride(viewer, subject).get().getSkin().get());
        assertSame(shared, registry.getDisguiseFor(other, subject).get());
    }

    @Test public void missingOverrideNeverGetsSynthesizedFromGlobal() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        Disguise global = new Disguise(subject, "GlobalAlias");
        registry.setDisguise(global);
        assertFalse(registry.getViewerOverride(viewer, subject).isPresent());
        assertTrue(registry.getDisguiseFor(viewer, subject).isPresent());
        assertFalse(new SkinAssignment(registry).applyForViewerIfCurrent(viewer, subject, global, skin));
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
