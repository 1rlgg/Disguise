package dev.fyre.disguise.protocol;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.core.InMemoryDisguiseService;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViewerAliasCollisionPolicyTest {
    @Test public void detectsViewerSpecificDuplicatesIgnoringUsernameCase() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        registry.setDisguiseFor(viewer, new Disguise(a, "AliasOne"));
        registry.setDisguiseFor(viewer, new Disguise(b, "aliasone"));
        assertTrue(ViewerAliasCollisionPolicy.conflicts(registry, viewer, a,
                "AliasOne", Arrays.asList(viewer, a, b)));
    }

    @Test public void ignoresOtherViewersAndSelfSubject() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), differentViewer = UUID.randomUUID();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        registry.setDisguiseFor(differentViewer, new Disguise(b, "AliasOne"));
        assertFalse(ViewerAliasCollisionPolicy.conflicts(registry, viewer, a,
                "AliasOne", Arrays.asList(a, b)));
        registry.setDisguiseFor(viewer, new Disguise(a, "AliasOne"));
        assertFalse(ViewerAliasCollisionPolicy.conflicts(registry, viewer, a,
                "AliasOne", Arrays.asList(viewer, a)));
    }

    @Test public void overridesCanRemoveAConflictForJustOneViewer() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewerA = UUID.randomUUID(), viewerB = UUID.randomUUID();
        UUID target = UUID.randomUUID(), other = UUID.randomUUID();
        registry.setDisguise(new Disguise(other, "SharedAlias"));
        registry.setDisguiseFor(viewerA, new Disguise(other, "SafeOther"));
        assertFalse(ViewerAliasCollisionPolicy.conflicts(registry, viewerA, target,
                "SharedAlias", Arrays.asList(target, other)));
        assertTrue(ViewerAliasCollisionPolicy.conflicts(registry, viewerB, target,
                "SharedAlias", Arrays.asList(target, other)));
    }

    @Test public void ignoresOfflineDisguisedSubjectsNotInObservedOnlineSet() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID(), offline = UUID.randomUUID();
        registry.setDisguise(new Disguise(offline, "SharedAlias"));
        assertFalse(ViewerAliasCollisionPolicy.conflicts(registry, viewer, target,
                "SharedAlias", Arrays.asList(viewer, target)));
    }

    @Test public void globalAliasCollisionsAreVisibleToEveryViewer() {
        InMemoryDisguiseService registry = new InMemoryDisguiseService();
        UUID viewer = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        registry.setDisguise(new Disguise(b, "SharedAlias"));
        assertTrue(ViewerAliasCollisionPolicy.conflicts(registry, viewer, a,
                "sharedalias", Arrays.asList(a, b)));
    }
}
