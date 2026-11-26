package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.SkinProperty;
import gg.rl1.disguise.core.InMemoryDisguiseService;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinAssignmentTest {
    private static final SkinProperty SIGNED = new SkinProperty("dGVzdA==", "c2ln");

    @Test public void appliesSkinAndKeepsName() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID();
        Disguise expected = new Disguise(subject, "AliasName");
        service.setDisguise(expected);
        assertTrue(new SkinAssignment(service).applyIfCurrent(subject, expected, SIGNED));
        assertEquals("AliasName", service.getDisguise(subject).get().getName());
        assertSame(SIGNED, service.getDisguise(subject).get().getSkin().get());
    }

    @Test public void ignoresRequestAfterAliasReplacement() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID();
        Disguise previous = new Disguise(subject, "OldAlias");
        service.setDisguise(previous);
        Disguise current = new Disguise(subject, "NewAlias");
        service.setDisguise(current);
        assertFalse(new SkinAssignment(service).applyIfCurrent(subject, previous, SIGNED));
        assertSame(current, service.getDisguise(subject).get());
    }

    @Test public void ignoresRequestAfterRemovalOrRecreation() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID();
        Disguise prior = new Disguise(subject, "AliasName");
        service.setDisguise(prior);
        service.removeDisguise(subject);
        service.setDisguise(new Disguise(subject, "AliasName"));
        assertFalse(new SkinAssignment(service).applyIfCurrent(subject, prior, SIGNED));
        assertFalse(service.getDisguise(subject).get().getSkin().isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsignedTextureRequest() {
        UUID subject = UUID.randomUUID();
        new SkinAssignment(new InMemoryDisguiseService()).applyIfCurrent(
                subject, new Disguise(subject, "AliasName"), new SkinProperty("dGVzdA==", null));
    }
}
