package gg.rl1.disguise.skin;

import gg.rl1.disguise.api.SkinProperty;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class MojangProfileParserTest {
    private final UUID id = UUID.fromString("c0c4ad72-895e-4d40-8554-fa70e82cf5ed");

    private String response(UUID requested) {
        return "{\"id\":\"" + requested.toString().replace("-", "")
                + "\",\"properties\":[{\"name\":\"textures\",\"value\":\"dGVzdA==\",\"signature\":\"c2ln\"}]}";
    }

    @Test public void acceptsMatchingProfileAndSignedTextures() throws Exception {
        SkinProperty property = MojangSkinSource.signedTextures(id, response(id));
        assertTrue(property.isSigned());
        assertEquals("dGVzdA==", property.getValue());
    }

    @Test(expected = IOException.class)
    public void rejectsAnotherAccountProfile() throws Exception {
        MojangSkinSource.signedTextures(id, response(UUID.randomUUID()));
    }

    @Test(expected = IOException.class)
    public void rejectsUnsignedTextureProperty() throws Exception {
        MojangSkinSource.signedTextures(id, "{\"id\":\""
                + id.toString().replace("-", "")
                + "\",\"properties\":[{\"name\":\"textures\",\"value\":\"dGVzdA==\"}]}");
    }

    @Test(expected = IOException.class)
    public void rejectsMalformedResponse() throws Exception {
        MojangSkinSource.signedTextures(id, "not-json");
    }

    @Test(expected = IOException.class)
    public void rejectsMissingId() throws Exception {
        MojangSkinSource.signedTextures(id,
                "{\"properties\":[{\"name\":\"textures\",\"value\":\"dGVzdA==\",\"signature\":\"c2ln\"}]}");
    }
}
