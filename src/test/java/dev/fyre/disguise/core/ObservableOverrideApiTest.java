package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.SkinProperty;
import dev.fyre.disguise.skin.SkinAssignment;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Public observable API must preserve the difference between an override and fallback. */
public class ObservableOverrideApiTest {
    @Test public void explicitOverrideSurvivesObservableWrapperEvenWhenSharedWithGlobal() {
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID(), other = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        ObservableDisguiseService service = new ObservableDisguiseService(
                registry, transition -> { }, () -> Collections.singletonList(viewer));
        Disguise shared = new Disguise(subject, "SharedAlias");
        service.setDisguise(shared);
        service.setDisguiseFor(viewer, shared);
        assertSame(shared, service.getViewerOverride(viewer, subject).get());
        assertFalse(service.getViewerOverride(other, subject).isPresent());
        assertSame(shared, service.getDisguiseFor(other, subject).get());

        SkinProperty signed = new SkinProperty("dGVzdA==", "c2ln");
        assertTrue(new SkinAssignment(service).applyForViewerIfCurrent(viewer, subject, shared, signed));
        assertSame(shared, service.getDisguise(subject).get());
        assertFalse(service.getDisguise(subject).get().getSkin().isPresent());
        assertSame(signed, service.getViewerOverride(viewer, subject).get().getSkin().get());
        assertSame(shared, service.getDisguiseFor(other, subject).get());
    }

    @Test public void missingOverrideCannotBeCreatedBySkinAssignment() {
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        ObservableDisguiseService service = new ObservableDisguiseService(
                registry, transition -> { }, () -> Collections.singletonList(viewer));
        Disguise global = new Disguise(subject, "GlobalAlias");
        service.setDisguise(global);
        assertFalse(new SkinAssignment(service).applyForViewerIfCurrent(viewer, subject, global,
                new SkinProperty("dGVzdA==", "c2ln")));
        assertFalse(service.getViewerOverride(viewer, subject).isPresent());
    }
}
