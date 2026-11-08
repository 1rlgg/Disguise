package dev.fyre.disguise.render;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.protocol.LegacyEntityRefresh;
import dev.fyre.disguise.protocol.EntityRefreshFailureGate;
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
 * Experimental tab-list and opt-in entity renderer. Tracks only successful
 * updates and never mutates the underlying Bukkit entity.
 */
public final class TabListIdentityRenderer implements IdentityRenderer {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;
    private final RefreshQueue pending;
    private final AppliedTabEntries applied = new AppliedTabEntries();
    private final DisguiseService state;
    private final LegacyEntityRefresh entityRefresh;
    private final EntityRefreshFailureGate entityGate = new EntityRefreshFailureGate(3);
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
        assertPrimaryThread();
        // On saturation, never prefer a cosmetic alias update to a removal.
        if (!transition.getAfter().isPresent()) {
            pending.offerRestoration(transition.getViewerId(), transition.getSubjectId());
        } else {
            enqueue(transition.getViewerId(), transition.getSubjectId());
        }
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
    public int pendingRestorationCount() { return pending.pendingRestorations(); }
    public long droppedCosmeticCount() { return pending.droppedCosmetic(); }
    public long displacedRestorationCount() { return pending.droppedRestoration(); }
    public boolean entityRefreshCircuitOpen() { return entityRefresh != null && !entityGate.enabled(); }
    public int appliedCount() { return applied.size(); }

    private void deliver(Player viewer, Player subject, boolean tryEntity) throws InvocationTargetException {
        if (!tryEntity || entityRefresh == null || !entityGate.enabled()) {
            transport.refresh(viewer, subject);
            return;
        }
        try {
            boolean changed = entityRefresh.refresh(viewer, subject);
            entityGate.success();
            if (!changed) transport.refresh(viewer, subject);
        } catch (InvocationTargetException | RuntimeException failure) {
            if (entityGate.failure()) {
                logger.warning("Entity refresh disabled after repeated errors; tab-only rendering remains enabled");
            }
            throw failure;
        }
    }

    public void flush(int maxPerTick) {
        assertPrimaryThread();
        // Defer excess entity work instead of silently downgrading it to tab-only updates.
        int batchSize = entityRefresh == null ? maxPerTick : Math.min(maxPerTick, 8);
        for (RefreshQueue.Pair pair : pending.poll(batchSize)) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()) continue;
            if (viewer.equals(subject) || !viewer.canSee(subject)) continue;
            try {
                deliver(viewer, subject, true);
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
                deliver(viewer, subject, true);
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
