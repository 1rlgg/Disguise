package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.packet.PlayerInfoPacketPolicy;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoRewritePolicyTest {
    @Test public void neverRewritesSelf() {
        UUID subject = UUID.randomUUID();
        assertFalse(PlayerInfoPacketPolicy.mayRewrite(subject, subject));
    }

    @Test public void mayRewriteOtherViewer() {
        assertTrue(PlayerInfoPacketPolicy.mayRewrite(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test(expected = NullPointerException.class)
    public void requiresViewerId() {
        PlayerInfoPacketPolicy.mayRewrite(null, UUID.randomUUID());
    }

    @Test(expected = NullPointerException.class)
    public void requiresSubjectId() {
        PlayerInfoPacketPolicy.mayRewrite(UUID.randomUUID(), null);
    }
}
