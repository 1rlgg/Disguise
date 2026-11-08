package dev.fyre.disguise.render;

import dev.fyre.disguise.protocol.PlayerInfoTabRefresh;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * An opt-in tab-list-only renderer. This is not a full disguise engine.
 * Disable on competitive servers until packet behavior is verified.
 */
public final class TabListIdentityRenderer implements IdentityRenderer {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override public void render(IdentityTransition transition) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Tab refresh requires primary thread");
        Player viewer = Bukkit.getPlayer(transition.getViewerId());
        Player subject = Bukkit.getPlayer(transition.getSubjectId());
        if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()) return;
        if (viewer.equals(subject) || !viewer.canSee(subject)) return;
        try {
            transport.refresh(viewer, subject);
        } catch (InvocationTargetException | RuntimeException ex) {
            logger.log(Level.WARNING, "Tab refresh failed for viewer " + viewer.getUniqueId()
                    + " subject " + subject.getUniqueId(), ex);
        }
    }
}
