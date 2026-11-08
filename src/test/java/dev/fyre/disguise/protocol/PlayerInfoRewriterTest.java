package dev.fyre.disguise.protocol;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerInfoRewriterTest {
    @Test public void appliesPerViewerAliasAndRestoresOriginalEntry() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        PlayerInfoRewriter rewriter = new PlayerInfoRewriter(registry);
        PlayerInfoData original = new PlayerInfoData(new WrappedGameProfile(subject, "Original"), 42,
                EnumWrappers.NativeGameMode.SURVIVAL, null);
        List<PlayerInfoData> originalList = Collections.singletonList(original);
        assertSame(originalList, rewriter.rewrite(viewer, originalList));
        registry.setDisguise(new Disguise(subject, "Alias"));
        List<PlayerInfoData> disguised = rewriter.rewrite(viewer, originalList);
        assertEquals("Alias", disguised.get(0).getProfile().getName());
        assertEquals(subject, disguised.get(0).getProfile().getUUID());
        assertEquals("Original", original.getProfile().getName());
        registry.removeDisguise(subject);
        assertSame(originalList, rewriter.rewrite(viewer, originalList));
    }
}
