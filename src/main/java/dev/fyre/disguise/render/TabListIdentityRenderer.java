package dev.fyre.disguise.render;

import dev.fyre.disguise.render.identity.IdentityRenderer;

import dev.fyre.disguise.render.identity.IdentityTransition;

import dev.fyre.disguise.render.refresh.RefreshWorkMeter;
import dev.fyre.disguise.render.refresh.DeferredRefreshBacklog;
import dev.fyre.disguise.render.refresh.RefreshRetryLedger;

import dev.fyre.disguise.render.restoration.RestorationReconciler;
import dev.fyre.disguise.render.restoration.AppliedTabEntries;

import dev.fyre.disguise.render.refresh.RefreshPolicy;

import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.protocol.LegacyEntityRefresh;
import dev.fyre.disguise.protocol.PacketRewriteBreaker;
import dev.fyre.disguise.protocol.EntityRefreshTransaction;
import dev.fyre.disguise.protocol.packet.PlayerInfoTabRefresh;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabListIdentityRenderer implements IdentityRenderer {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;
    private final RefreshQueue pending;
    private final AppliedTabEntries applied = new AppliedTabEntries();
    private final java.util.Set<RefreshQueue.Pair> manualRestores =
            new java.util.HashSet<RefreshQueue.Pair>();
    private final RefreshRetryLedger retries;
    private final DeferredRefreshBacklog deferred;
    private final RestorationReconciler reconciler = new RestorationReconciler();
    private final RefreshWorkMeter workMeter = new RefreshWorkMeter();
    private final RestorationReconciler shutdownBudget = new RestorationReconciler(256);
    private int ticksSinceReconcile;
    private final DisguiseService state;
    private final LegacyEntityRefresh entityRefresh;
    private final PacketRewriteBreaker entityGate = new PacketRewriteBreaker(3);
    private final EntityRefreshTransaction entityAttempt = new EntityRefreshTransaction(entityGate);
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

    public boolean requestOriginalRestoration(UUID viewer, UUID subject) {
        assertPrimaryThread();
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        boolean current = state != null && state.getDisguiseFor(viewer, subject).isPresent();
        if (!RefreshPolicy.manualRestorationAllowed(applied.contains(viewer, subject),
                current, viewer.equals(subject))) return false;
        retries.newTransition(new RefreshQueue.Pair(viewer, subject));
        if (!enqueueRestoration(viewer, subject)) return false;
        manualRestores.add(new RefreshQueue.Pair(viewer, subject));
        return true;
    }

    public boolean wasApplied(UUID viewer, UUID subject) {
        assertPrimaryThread();
        return applied.contains(viewer, subject);
    }

    public void enqueue(UUID viewer, UUID subject) {
        assertPrimaryThread();

        if (state != null && applied.contains(viewer, subject)
                && !state.getDisguiseFor(viewer, subject).isPresent()) {
            enqueueRestoration(viewer, subject);
            return;
        }
        if (state != null && state.getDisguiseFor(viewer, subject).isPresent())
            manualRestores.remove(new RefreshQueue.Pair(viewer, subject));
        if (pending.offer(viewer, subject)) {

            deferred.cancel(viewer, subject);
        } else {
            if (!deferred.offer(viewer, subject) && !capacityWarning) {
                capacityWarning = true;
                logger.warning("Tab refresh and deferred queues full; cosmetic updates may be dropped");
            }
        }
    }

    private boolean enqueueRestoration(UUID viewer, UUID subject) {
        deferred.cancel(viewer, subject);
        return pending.offerRestoration(viewer, subject, evicted ->
                deferred.offer(evicted.getViewer(), evicted.getSubject()));
    }

    @Override public void render(IdentityTransition transition) {
        assertPrimaryThread();

        RefreshQueue.Pair changedPair = new RefreshQueue.Pair(
                transition.getViewerId(), transition.getSubjectId());
        manualRestores.remove(changedPair);

        retries.newTransition(changedPair);

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
        manualRestores.removeIf(pair -> id.equals(pair.getViewer()) || id.equals(pair.getSubject()));
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
    public boolean entityRefreshCircuitOpen() { return entityRefresh != null && entityGate.isOpen(); }
    public int appliedCount() { return applied.size(); }

    private void deliver(Player viewer, Player subject, boolean tryEntity) throws InvocationTargetException {
        if (!tryEntity || entityRefresh == null || entityGate.isOpen()) {
            transport.refresh(viewer, subject);
            return;
        }
        try {
            entityAttempt.deliver(() -> entityRefresh.refresh(viewer, subject),
                    () -> transport.refresh(viewer, subject));
        } catch (InvocationTargetException | RuntimeException failure) {
            for (Throwable suppressed : failure.getSuppressed()) {
                if (suppressed instanceof EntityRefreshTransaction.EntityCircuitOpenedException) {
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
                        return RefreshPolicy.canRestore(true,
                                state.getDisguiseFor(candidate.getViewer(), candidate.getSubject()).isPresent(),
                                viewer.isOnline(), subject.isOnline(), viewer.canSee(subject),
                                viewer.equals(subject));
                    },
                    128)) {
                enqueueRestoration(pair.getViewer(), pair.getSubject());
            }
        }

        int batchSize = RefreshPolicy.batchSize(maxPerTick,
                entityRefresh != null && !entityGate.isOpen());
        long started = System.nanoTime();
        java.util.List<RefreshQueue.Pair> work = pending.poll(batchSize);
        for (RefreshQueue.Pair pair : work) {
            boolean manualRestore = manualRestores.remove(pair);

            if (!RefreshPolicy.mayAttempt(retries.suppressed(pair))) continue;
            Player viewer = Bukkit.getPlayer(pair.getViewer());
            Player subject = Bukkit.getPlayer(pair.getSubject());
            if (viewer == null || subject == null || !viewer.isOnline() || !subject.isOnline()
                    || viewer.equals(subject) || !viewer.canSee(subject)) {
                retries.succeeded(pair);
                continue;
            }

            boolean currentDisguise = state != null
                    && state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent();
            if (state != null && !RefreshPolicy.shouldSend(
                    currentDisguise, applied.contains(pair.getViewer(), pair.getSubject()))) {
                retries.succeeded(pair);
                continue;
            }
            try {
                deliver(viewer, subject, !manualRestore);
                retries.succeeded(pair);
                if (state != null) {
                    if (state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent()) {
                        applied.applied(pair.getViewer(), pair.getSubject());
                    } else {
                        applied.restored(pair.getViewer(), pair.getSubject());
                    }
                }
            } catch (InvocationTargetException | RuntimeException ex) {

                if (RefreshPolicy.keepForRestore(currentDisguise,
                        applied.contains(pair.getViewer(), pair.getSubject()))) {
                    applied.applied(pair.getViewer(), pair.getSubject());
                }
                boolean retry = retries.shouldRetry(pair);
                if (retry) {

                    boolean restore = state != null
                            && !state.getDisguiseFor(pair.getViewer(), pair.getSubject()).isPresent();
                    if (restore) {
                        if (enqueueRestoration(pair.getViewer(), pair.getSubject()) && manualRestore)
                            manualRestores.add(pair);
                    }
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
        manualRestores.clear();
        deferred.clear();
        retries.clear();
        reconciler.reset();
        workMeter.reset();
        ticksSinceReconcile = 0;
        applied.clear();
        capacityWarning = false;
    }
}
