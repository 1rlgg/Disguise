package dev.fyre.disguise.render;

import dev.fyre.disguise.api.DisguiseService;
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
 * Experimental tab-list-only renderer. Per-tick updates are capped; world
 * entities are never hidden, destroyed or respawned.
 */
public final class TabListIdentityRenderer implements IdentityRenderer {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;
    private final RefreshQueue pending;
    private final AppliedTabEntries applied = new AppliedTabEntries();
    private final DisguiseService state;
    private boolean capacityWarning;

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger) {
        this(transport, logger, 2048, null);
    }

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger, int maxQueue) {
        this(transport, logger, maxQueue, null);
    }

    public TabListIdentityRenderer(PlayerInfoTabRefresh transport, Logger logger, int maxQueue,
                                   DisguiseService state) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.pending = new RefreshQueue(maxQueue);
        this.state = state;
    }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Tab refresh requires primary thread");
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

    public void flush(int maxPerTick) {
        assertPrimaryThread();
        for (RefreshQueue.Pair pair : pending.poll(maxPerTick)) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()) continue;
            if (viewer.equals(subject) || !viewer.canSee(subject)) continue;
            try {
                transport.refresh(viewer, subject);
                if (state != null) {
                    if (state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent()) {
                        applied.applied(pair.getViewer(), pair.getSubject());
                    } else {
                        applied.restored(pair.getViewer(), pair.getSubject());
                    }
                }
            } catch (InvocationTargetException | RuntimeException ex) {
                logger.log(Level.WARNING, "Tab refresh failed for viewer " + viewer.getUniqueId()
                        + " subject " + subject.getUniqueId(), ex);
            }
        }
        if (pending.size() == 0) capacityWarning = false;
    }

    /**
     * Best effort: must be called only after disguise registry is cleared,
     * before ProtocolLib is unregistered. No entity/nametag restoration.
     */
    public void restoreApplied() {
        assertPrimaryThread();
        for (RefreshQueue.Pair pair : applied.snapshot()) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()
                    || viewer.equals(subject) || !viewer.canSee(subject)) continue;
            try {
                transport.refresh(viewer, subject);
                applied.restored(pair.getViewer(), pair.getSubject());
            } catch (InvocationTargetException | RuntimeException ex) {
                logger.log(Level.WARNING, "Could not restore tab entry for viewer " + viewer.getUniqueId(), ex);
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
