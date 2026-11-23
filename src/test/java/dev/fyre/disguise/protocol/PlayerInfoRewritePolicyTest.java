package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.packet.PlayerInfoRewriter;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoRewritePolicyTest {
    @Test public void neverRewritesSelf() {
        UUID subject = UUID.randomUUID();
        assertFalse(PlayerInfoRewriter.mayRewrite(subject, subject));
    }

    @Test public void mayRewriteOtherViewer() {
        assertTrue(PlayerInfoRewriter.mayRewrite(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test(expected = NullPointerException.class)
    public void requiresViewerId() {
        PlayerInfoRewriter.mayRewrite(null, UUID.randomUUID());
    }

    @Test(expected = NullPointerException.class)
    public void requiresSubjectId() {
        PlayerInfoRewriter.mayRewrite(UUID.randomUUID(), null);
    }
}
