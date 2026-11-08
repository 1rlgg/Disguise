package dev.fyre.disguise.api;

import org.junit.Test;
import static org.junit.Assert.*;

public class SkinPropertyEncodingTest {
    @Test public void acceptsCanonicalSignedData() {
        SkinProperty property = new SkinProperty("dGVzdA==", "c2ln");
        assertEquals("dGVzdA==", property.getValue());
        assertTrue(property.isSigned());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRequiredPadding() {
        new SkinProperty("dGVzdA", "c2ln");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidAlphabet() {
        new SkinProperty("not-base64!", "c2ln");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedPayload() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 33000; i++) builder.append('A');
        new SkinProperty(builder.toString(), "c2ln");
    }
}
