package dev.fyre.disguise.protocol;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoRewritePolicyTest {
    @Test public void neverRewritesSelf() {
        UUID subject = UUID.randomUUID();
        assertFalse(PlayerInfoRewritePolicy.mayRewrite(subject, subject));
    }

    @Test public void mayRewriteOtherViewer() {
        assertTrue(PlayerInfoRewritePolicy.mayRewrite(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test(expected = NullPointerException.class)
    public void requiresViewerId() {
        PlayerInfoRewritePolicy.mayRewrite(null, UUID.randomUUID());
    }

    @Test(expected = NullPointerException.class)
    public void requiresSubjectId() {
        PlayerInfoRewritePolicy.mayRewrite(UUID.randomUUID(), null);
    }
}
