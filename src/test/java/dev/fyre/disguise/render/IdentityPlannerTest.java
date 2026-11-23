package dev.fyre.disguise.render;

import dev.fyre.disguise.render.identity.IdentityTransition;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.SkinProperty;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdentityPlannerTest {
    private final UUID viewer = UUID.randomUUID(), subject = UUID.randomUUID();

    @Test public void skipsSemanticallyIdenticalDisguises() {
        assertFalse(IdentityTransition.plan(viewer, subject, new Disguise(subject, "Alias"), new Disguise(subject, "Alias")).isPresent());
    }

    @Test public void emitsRestoreOnRemoval() {
        IdentityTransition transition = IdentityTransition.plan(viewer, subject, new Disguise(subject, "Alias"), null).get();
        assertFalse(transition.getAfter().isPresent());
        assertEquals(viewer, transition.getViewerId());
    }

    @Test public void detectsSkinChange() {
        Disguise first = new Disguise(subject, "Alias", new SkinProperty("dGVzdA==", "c2ln"));
        Disguise second = new Disguise(subject, "Alias", new SkinProperty("bmV3", "c2ln"));
        assertTrue(IdentityTransition.plan(viewer, subject, first, second).isPresent());
    }
}
