package gg.rl1.disguise.render;

import gg.rl1.disguise.api.DisguiseService;
import gg.rl1.disguise.protocol.EntityRefreshTransaction;
import gg.rl1.disguise.protocol.LegacyEntityRefresh;
import gg.rl1.disguise.protocol.PacketRewriteBreaker;
import gg.rl1.disguise.protocol.packet.PlayerInfoTabRefresh;
import gg.rl1.disguise.render.identity.IdentityTransition;
import gg.rl1.disguise.render.refresh.DeferredRefreshBacklog;
import gg.rl1.disguise.render.refresh.RefreshPolicy;
import gg.rl1.disguise.render.refresh.RefreshRetryLedger;
import gg.rl1.disguise.render.refresh.RefreshWorkMeter;
import gg.rl1.disguise.render.restoration.AppliedTabEntries;
import gg.rl1.disguise.render.restoration.RestorationReconciler;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabListIdentityRenderer implements Consumer<IdentityTransition> {
    private final PlayerInfoTabRefresh transport;
    private final Logger logger;
    private final RefreshQueue pending;
    private final AppliedTabEntries applied = new AppliedTabEntries();
    private final Set<RefreshQueue.Pair> manualRestores =
            new HashSet<RefreshQueue.Pair>();
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

        RefreshQueue.Pair pair = new RefreshQueue.Pair(viewer, subject);
        if (viewer.equals(subject)) return;
        boolean currentDisguise = state != null && state.getDisguiseFor(viewer, subject).isPresent();
        if (state != null && !currentDisguise) {
            if (applied.contains(pair)) enqueueRestoration(viewer, subject);
            return;
        }
        if (currentDisguise && !manualRestores.isEmpty())
            manualRestores.remove(pair);
        if (pending.offer(pair)) {
            if (deferred.size() > 0) deferred.cancel(pair);
        } else {
            if (!deferred.offer(pair) && !capacityWarning) {
                capacityWarning = true;
                logger.warning("Tab refresh and deferred queues full; cosmetic updates may be dropped");
            }
        }
    }

    private boolean enqueueRestoration(UUID viewer, UUID subject) {
        boolean admitted = pending.offerRestoration(viewer, subject, deferred::offer);
        if (admitted && deferred.size() > 0) deferred.cancel(viewer, subject);
        return admitted;
    }

    @Override
    public void accept(IdentityTransition transition) {
        render(transition);
    }

    public void render(IdentityTransition transition) {
        assertPrimaryThread();

        RefreshQueue.Pair changedPair = new RefreshQueue.Pair(
                transition.getViewerId(), transition.getSubjectId());
        manualRestores.remove(changedPair);

        retries.newTransition(changedPair);

        if (!transition.getAfter().isPresent()) {
            if (state != null && !applied.contains(changedPair)) {
                pending.cancel(changedPair);
                deferred.cancel(changedPair);
                return;
            }
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
        if (state != null && applied.size() > 0) {
            if (++ticksSinceReconcile >= 20) {
                ticksSinceReconcile = 0;
                for (RefreshQueue.Pair pair : reconciler.scan(applied.scanSnapshot(),
                    candidate -> {
                        if (retries.suppressed(candidate) || state.getDisguiseFor(candidate.getViewer(), candidate.getSubject()).isPresent()) return false;
                        Player viewer = Bukkit.getPlayer(candidate.getViewer());
                        Player subject = Bukkit.getPlayer(candidate.getSubject());
                        if (viewer == null || subject == null) return false;
                        return RefreshPolicy.canRestore(true,
                                false,
                                viewer.isOnline(), subject.isOnline(), viewer.canSee(subject),
                                viewer.equals(subject));
                    },
                    128)) {
                    enqueueRestoration(pair.getViewer(), pair.getSubject());
                }
            }
        } else {
            ticksSinceReconcile = 0;
        }

        if (pending.size() == 0 && deferred.size() == 0) {
            capacityWarning = false;
            return;
        }

        int batchSize = RefreshPolicy.batchSize(maxPerTick,
                entityRefresh != null && !entityGate.isOpen());
        long started = System.nanoTime();
        if (pending.size() == 0) deferred.drainTo(pending, batchSize);
        List<RefreshQueue.Pair> work = pending.poll(batchSize);
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
                    currentDisguise, applied.contains(pair))) {
                retries.succeeded(pair);
                continue;
            }
            try {
                deliver(viewer, subject, !manualRestore);
                retries.succeeded(pair);
                if (state != null) {
                    if (currentDisguise) applied.applied(pair);
                    else applied.restored(pair);
                }
            } catch (InvocationTargetException | RuntimeException ex) {

                if (RefreshPolicy.keepForRestore(currentDisguise,
                        applied.contains(pair))) {
                    applied.applied(pair);
                }
                boolean retry = retries.shouldRetry(pair);
                if (retry) {

                    boolean restore = state != null && !currentDisguise;
                    if (restore) {
                        boolean admitted = enqueueRestoration(pair.getViewer(), pair.getSubject());
                        if (admitted && manualRestore) manualRestores.add(pair);
                        if (!admitted) {
                            retries.succeeded(pair);
                            retry = false;
                        }
                    }
                    else if (!pending.offer(pair)
                            && !deferred.offer(pair)) {
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
        Set<RefreshQueue.Pair> remaining = applied.snapshot();
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
                applied.restored(pair);
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
