package dev.fyre.disguise.skin;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.SkinProperty;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkinAssignmentIsolationTest {
    @Test public void globalSkinUpdatePreservesViewerSpecificAlias() {
        InMemoryDisguiseService service = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID(), another = UUID.randomUUID();
        Disguise global = new Disguise(subject, "GlobalAlias");
        Disguise custom = new Disguise(subject, "PrivateAlias");
        service.setDisguise(global);
        service.setDisguiseFor(viewer, custom);
        SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");
        assertTrue(new SkinAssignment(service).applyIfCurrent(subject, global, skin));
        assertSame(custom, service.getDisguiseFor(viewer, subject).get());
        assertEquals("PrivateAlias", service.getDisguiseFor(viewer, subject).get().getName());
        assertSame(skin, service.getDisguiseFor(another, subject).get().getSkin().get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void skinAssignmentCannotWriteToWrongPlayerId() {
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        new SkinAssignment(new InMemoryDisguiseService()).applyIfCurrent(
                one, new Disguise(two, "AliasName"), new SkinProperty("dGVzdA==", "c2ln"));
    }
}
