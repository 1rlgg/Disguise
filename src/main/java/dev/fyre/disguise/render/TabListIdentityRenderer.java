package dev.fyre.disguise.render;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.protocol.LegacyEntityRefresh;
import dev.fyre.disguise.protocol.PlayerInfoTabRefresh;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Experimental tab-list renderer, optionally with 1.8 client entity respawn.
 * Both paths are disabled by default on production servers.
 */
public final class TabListIdentityRenderer implements IdentityRenderer {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;
    private final RefreshQueue pending;
    private final AppliedTabEntries applied = new AppliedTabEntries();
    private final DisguiseService state;
    private final LegacyEntityRefresh entityRefresh;
    private boolean capacityWarning;

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger) {
        this(transport, logger, 2048, null, null);
    }

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger, int maxQueue) {
        this(transport, logger, maxQueue, null, null);
    }

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger, int maxQueue,
                                   DisguiseService state) {
        this(transport, logger, maxQueue, state, null);
    }

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger, int maxQueue,
                                   DisguiseService state, LegacyEntityRefresh entityRefresh) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.pending = new RefreshQueue(maxQueue);
        this.state = state;
        this.entityRefresh = entityRefresh;
    }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Refresh requires primary thread");
    }

    public void enqueue(UUID viewer, UUID subject) {
        assertPrimaryThread();
        if (!pending.offer(viewer, subject) && !capacityWarning) {
            capacityWarning = true;
            logger.warning("Tab refresh queue full; dropping updates until backlog drains");
        }
    }

    @Override public void render(IdentityTransition transition) {
        enqueue(transition.getViewerId(), transition.getSubjectId());
    }

    public void refreshViewer(UUID viewerId, Collection<UUID> subjects) {
        assertPrimaryThread();
        for (UUID subject : subjects) enqueue(viewerId, subject);
    }

    public void refreshSubject(UUID subjectId, Collection<UUID> viewers) {
        assertPrimaryThread();
        for (UUID viewer : viewers) enqueue(viewer, subjectId);
    }

    public void forget(UUID id) {
        assertPrimaryThread();
        pending.forget(id);
        applied.forget(id);
    }

    public int pendingCount() { return pending.size(); }
    public int appliedCount() { return applied.size(); }

    private void deliver(Player viewer, Player subject) throws InvocationTargetException {
        if (entityRefresh == null || !entityRefresh.refresh(viewer, subject)) {
            transport.refresh(viewer, subject);
        }
    }

    public void flush(int maxPerTick) {
        assertPrimaryThread();
        for (RefreshQueue.Pair pair : pending.poll(maxPerTick)) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()) continue;
            if (viewer.equals(subject) || !viewer.canSee(subject)) continue;
            try {
                deliver(viewer, subject);
                if (state != null) {
                    if (state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent()) {
                        applied.applied(pair.getViewer(), pair.getSubject());
                    } else {
                        applied.restored(pair.getViewer(), pair.getSubject());
                    }
                }
            } catch (InvocationTargetException | RuntimeException ex) {
                logger.log(Level.WARNING, "Refresh failed for viewer " + viewer.getUniqueId()
                        + " subject " + subject.getUniqueId(), ex);
            }
        }
        if (pending.size() == 0) capacityWarning = false;
    }

    public void restoreApplied() {
        assertPrimaryThread();
        for (RefreshQueue.Pair pair : applied.snapshot()) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()
                    || viewer.equals(subject) || !viewer.canSee(subject)) continue;
            try {
                deliver(viewer, subject);
                applied.restored(pair.getViewer(), pair.getSubject());
            } catch (InvocationTargetException | RuntimeException ex) {
                logger.log(Level.WARNING, "Could not restore appearance for viewer " + viewer.getUniqueId(), ex);
            }
        }
    }

    public void clear() {
        assertPrimaryThread();
        pending.clear();
        applied.clear();
        capacityWarning = false;
    }
}
