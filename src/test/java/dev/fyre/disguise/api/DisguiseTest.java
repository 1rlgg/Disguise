package dev.fyre.disguise.api;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DisguiseTest {
    @Test public void supportsOptionalSkinAndLegacyConstructor() {
        UUID id = UUID.randomUUID();
        assertFalse(new Disguise(id, "Player123").getSkin().isPresent());
        SkinProperty skin = new SkinProperty("dGVzdA==", "c2ln");
        assertSame(skin, new Disguise(id, "Player123", skin).getSkin().get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedNames() {
        new Disguise(UUID.randomUUID(), "ThisNameIsWayTooLong");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedSkinData() {
        new SkinProperty("not-base64!", "c2ln");
    }
}
