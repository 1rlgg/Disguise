package dev.fyre.disguise.render;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.protocol.LegacyEntityRefresh;
import dev.fyre.disguise.protocol.EntityRefreshFailureGate;
import dev.fyre.disguise.protocol.EntityRefreshAttempt;
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
    private final RefreshRetryLedger retries;
    private final DeferredRefreshBacklog deferred;
    private final RestorationReconciler reconciler = new RestorationReconciler();
    private final RefreshWorkMeter workMeter = new RefreshWorkMeter();
    private final ShutdownRestorationBudget shutdownBudget = new ShutdownRestorationBudget(256);
    private int ticksSinceReconcile;
    private final DisguiseService state;
    private final LegacyEntityRefresh entityRefresh;
    private final EntityRefreshFailureGate entityGate = new EntityRefreshFailureGate(3);
    private final EntityRefreshAttempt entityAttempt = new EntityRefreshAttempt(entityGate);
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
        this.retries = new RefreshRetryLedger(2, maxQueue);
        this.deferred = new DeferredRefreshBacklog(maxQueue);
        this.state = state;
        this.entityRefresh = entityRefresh;
    }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Refresh requires primary thread");
    }

    public boolean wasApplied(UUID viewer, UUID subject) {
        assertPrimaryThread();
        return applied.contains(viewer, subject);
    }

    public void enqueue(UUID viewer, UUID subject) {
        assertPrimaryThread();
        // A delayed resync may run after undisguise. Its old applied alias
        // must be restored urgently, not treated as expendable cosmetics.
        if (state != null && applied.contains(viewer, subject)
                && !state.getDisguiseFor(viewer, subject).isPresent()) {
            enqueueRestoration(viewer, subject);
            return;
        }
        if (pending.offer(viewer, subject)) {
            // A newer primary update supersedes an older deferred copy.
            deferred.cancel(viewer, subject);
        } else {
            if (!deferred.offer(viewer, subject) && !capacityWarning) {
                capacityWarning = true;
                logger.warning("Tab refresh and deferred queues full; cosmetic updates may be dropped");
            }
        }
    }

    private void enqueueRestoration(UUID viewer, UUID subject) {
        deferred.cancel(viewer, subject);
        pending.offerRestoration(viewer, subject, evicted ->
                deferred.offer(evicted.getViewer(), evicted.getSubject()));
    }

    @Override public void render(IdentityTransition transition) {
        assertPrimaryThread();
        // Explicit new identity transitions can retry a previously quarantined
        // pair; periodic background reconciliation cannot.
        retries.newTransition(new RefreshQueue.Pair(transition.getViewerId(), transition.getSubjectId()));
        // On saturation, never prefer a cosmetic alias update to a removal.
        if (!transition.getAfter().isPresent()) {
            enqueueRestoration(transition.getViewerId(), transition.getSubjectId());
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
        deferred.forget(id);
        retries.forget(id);
        applied.forget(id);
    }

    public int pendingCount() { return pending.size(); }
    public int deferredCount() { return deferred.size(); }
    public long deferredOverflowCount() { return deferred.overflowCount(); }
    public int pendingRestorationCount() { return pending.pendingRestorations(); }
    public int retryCount() { return retries.size(); }
    public long measuredFlushes() { return workMeter.flushes(); }
    public long measuredPairs() { return workMeter.pairs(); }
    public long averageFlushMicros() { return workMeter.averageMicros(); }
    public long peakFlushMicros() { return workMeter.peakMicros(); }
    public long exhaustedRetryCount() { return retries.exhaustedCount(); }
    public long rejectedRetryCount() { return retries.overflowCount(); }
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
            entityAttempt.deliver(() -> entityRefresh.refresh(viewer, subject),
                    () -> transport.refresh(viewer, subject));
        } catch (InvocationTargetException | RuntimeException failure) {
            for (Throwable suppressed : failure.getSuppressed()) {
                if (suppressed instanceof EntityRefreshAttempt.EntityCircuitOpenedException) {
                    logger.warning("Entity refresh disabled after repeated errors; tab-only rendering remains enabled");
                    break;
                }
            }
            throw failure;
        }
    }

    public void flush(int maxPerTick) {
        assertPrimaryThread();
        if (++ticksSinceReconcile >= 20 && state != null) {
            ticksSinceReconcile = 0;
            for (RefreshQueue.Pair pair : reconciler.scan(applied.snapshot(),
                    candidate -> {
                        if (retries.suppressed(candidate)) return false;
                        Player viewer = Bukkit.getPlayer(candidate.getViewer());
                        Player subject = Bukkit.getPlayer(candidate.getSubject());
                        if (viewer == null || subject == null) return false;
                        return RestorationEligibility.canRestore(true,
                                state.getDisguiseFor(candidate.getViewer(), candidate.getSubject()).isPresent(),
                                viewer.isOnline(), subject.isOnline(), viewer.canSee(subject),
                                viewer.equals(subject));
                    },
                    128)) {
                enqueueRestoration(pair.getViewer(), pair.getSubject());
            }
        }
        // Defer excess entity work instead of silently downgrading it to tab-only updates.
        int batchSize = RefreshBatchPolicy.size(maxPerTick,
                entityRefresh != null && entityGate.enabled());
        long started = System.nanoTime();
        java.util.List<RefreshQueue.Pair> work = pending.poll(batchSize);
        for (RefreshQueue.Pair pair : work) {
            // A stale deferred/resync queue entry is not a new identity
            // transition. Quarantine exhausted transports until an explicit
            // transition resets the retry gate.
            if (!RefreshAdmissionPolicy.mayAttempt(retries.suppressed(pair))) continue;
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()
                    || viewer.equals(subject) || !viewer.canSee(subject)) {
                retries.succeeded(pair);
                continue;
            }
            // A set/clear burst may leave nothing to display. Never remove/add
            // a normal player's tab entry unless this plugin actually applied
            // an alias before or a disguise remains desired.
            boolean currentDisguise = state != null
                    && state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent();
            if (state != null && !RefreshDeliveryPolicy.shouldSend(
                    currentDisguise, applied.contains(pair.getViewer(), pair.getSubject()))) {
                retries.succeeded(pair);
                continue;
            }
            try {
                deliver(viewer, subject, true);
                retries.succeeded(pair);
                if (state != null) {
                    if (state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent()) {
                        applied.applied(pair.getViewer(), pair.getSubject());
                    } else {
                        applied.restored(pair.getViewer(), pair.getSubject());
                    }
                }
            } catch (InvocationTargetException | RuntimeException ex) {
                // A send can fail after the client received REMOVE or ADD.
                // Conservatively remember the pair as possibly modified so
                // undisguise and shutdown can still attempt real-profile restore.
                if (RefreshFailureStatePolicy.keepForRestore(currentDisguise,
                        applied.contains(pair.getViewer(), pair.getSubject()))) {
                    applied.applied(pair.getViewer(), pair.getSubject());
                }
                boolean retry = retries.shouldRetry(pair);
                if (retry) {
                    // Never retry in this same flush; defer until the next tick.
                    // Urgent undisguise restoration gets precedence over cosmetics.
                    boolean restore = state != null
                            && !state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent();
                    if (restore) enqueueRestoration(pair.getViewer(), pair.getSubject());
                    else if (!pending.offer(pair.getViewer(), pair.getSubject())
                            && !deferred.offer(pair.getViewer(), pair.getSubject())) {
                        retries.succeeded(pair);
                        retry = false;
                    }
                }
                logger.log(Level.WARNING, (retry ? "Retrying" : "Giving up on")
                        + " failed refresh for viewer " + viewer.getUniqueId()
                        + " subject " + subject.getUniqueId(), ex);
            }
        }
        // After current work, refill a bounded number of normal slots.
        // Urgent restorations already in the primary queue retain priority.
        deferred.drainTo(pending, 32);
        workMeter.record(work.size(), Math.max(0L, System.nanoTime() - started));
        if (pending.size() == 0 && deferred.size() == 0) capacityWarning = false;
    }

    public void restoreApplied() {
        assertPrimaryThread();
        int restored = 0;
        int failed = 0;
        int unavailable = 0;
        java.util.Set<RefreshQueue.Pair> remaining = applied.snapshot();
        int omitted = shutdownBudget.omitted(remaining.size());
        for (RefreshQueue.Pair pair : shutdownBudget.selectPrioritized(
                remaining, pending.pendingRestorationSnapshot())) {
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()
                    || viewer.equals(subject) || !viewer.canSee(subject)) {
                unavailable++;
                continue;
            }
            try {
                // During shutdown never destroy and manually respawn world
                // entities. The server is about to tear down our listeners,
                // and tab-only restoration avoids ghost entities and armor
                // packet bursts in the final server tick.
                transport.restoreOriginal(viewer, subject);
                applied.restored(pair.getViewer(), pair.getSubject());
                restored++;
            } catch (InvocationTargetException | RuntimeException ex) {
                failed++;
                if (failed == 1) logger.log(Level.WARNING,
                        "Tab identity restoration failed during plugin disable (further errors summarized)", ex);
            }
        }
        if (failed > 0 || unavailable > 0 || omitted > 0) {
            logger.warning("Shutdown restoration: " + restored + " tab entries attempted successfully; "
                    + failed + " packet failures; " + unavailable
                    + " unavailable/hidden pairs; " + omitted
                    + " not attempted due to safety budget. Restart/reconnect clients if stale identities remain.");
        }
    }

    public void clear() {
        assertPrimaryThread();
        pending.clear();
        deferred.clear();
        retries.clear();
        reconciler.reset();
        workMeter.reset();
        ticksSinceReconcile = 0;
        applied.clear();
        capacityWarning = false;
    }
}
