package gg.rl1.disguise.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DisguiseDiagnostics {
    private final boolean tabEnabled;
    private final boolean entityEnabled;
    private final int pendingRefreshes;
    private final int appliedEntries;
    private final int trackedSpawns;
    private final int pendingSkins;
    private final int urgentRestorations;
    private final long lostCosmetic;
    private final long lostRestorations;
    private final boolean entityCircuitOpen;
    private final int queuedRetries;
    private final long exhaustedRetries;
    private final long rejectedRetries;
    private final int coolingDownSkins;
    private final long flushes;
    private final long processedPairs;
    private final long averageMicros;
    private final long peakMicros;
    private final int deferredCount;
    private final long deferredOverflows;

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries, trackedSpawns, pendingSkins,
                0, 0, 0, false, 0, 0, 0, 0);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries, trackedSpawns, pendingSkins,
                urgentRestorations, lostCosmetic, lostRestorations, entityCircuitOpen, 0, 0, 0, 0);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen,
                              int queuedRetries, long exhaustedRetries, long rejectedRetries) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries, trackedSpawns, pendingSkins,
                urgentRestorations, lostCosmetic, lostRestorations, entityCircuitOpen,
                queuedRetries, exhaustedRetries, rejectedRetries, 0);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen,
                              int queuedRetries, long exhaustedRetries, long rejectedRetries,
                              int coolingDownSkins) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries,
                trackedSpawns, pendingSkins, urgentRestorations, lostCosmetic,
                lostRestorations, entityCircuitOpen, queuedRetries, exhaustedRetries,
                rejectedRetries, coolingDownSkins, 0, 0, 0, 0);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen,
                              int queuedRetries, long exhaustedRetries, long rejectedRetries,
                              int coolingDownSkins, long flushes, long processedPairs,
                              long averageMicros, long peakMicros) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries, trackedSpawns, pendingSkins,
                urgentRestorations, lostCosmetic, lostRestorations, entityCircuitOpen,
                queuedRetries, exhaustedRetries, rejectedRetries, coolingDownSkins,
                flushes, processedPairs, averageMicros, peakMicros, 0, 0);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen,
                              int queuedRetries, long exhaustedRetries, long rejectedRetries,
                              int coolingDownSkins, long flushes, long processedPairs,
                              long averageMicros, long peakMicros,
                              int deferredCount, long deferredOverflows) {
        if (deferredCount < 0 || deferredOverflows < 0)
            throw new IllegalArgumentException("Deferred refresh counters cannot be negative");
        if (flushes < 0 || processedPairs < 0 || averageMicros < 0 || peakMicros < 0)
            throw new IllegalArgumentException("Refresh timing metrics cannot be negative");
        if (coolingDownSkins < 0) throw new IllegalArgumentException("Cooldown count cannot be negative");
        if (queuedRetries < 0 || exhaustedRetries < 0 || rejectedRetries < 0) {
            throw new IllegalArgumentException("Retry counters cannot be negative");
        }
        if (urgentRestorations < 0 || lostCosmetic < 0 || lostRestorations < 0) {
            throw new IllegalArgumentException("Counters cannot be negative");
        }
        if (entityEnabled && !tabEnabled) throw new IllegalArgumentException("Entity refresh requires tab refresh");
        if (pendingRefreshes < 0 || appliedEntries < 0 || trackedSpawns < 0 || pendingSkins < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
        this.tabEnabled = tabEnabled;
        this.entityEnabled = entityEnabled;
        this.pendingRefreshes = pendingRefreshes;
        this.appliedEntries = appliedEntries;
        this.trackedSpawns = trackedSpawns;
        this.pendingSkins = pendingSkins;
        this.urgentRestorations = urgentRestorations;
        this.lostCosmetic = lostCosmetic;
        this.lostRestorations = lostRestorations;
        this.entityCircuitOpen = entityCircuitOpen;
        this.queuedRetries = queuedRetries;
        this.exhaustedRetries = exhaustedRetries;
        this.rejectedRetries = rejectedRetries;
        this.coolingDownSkins = coolingDownSkins;
        this.flushes = flushes;
        this.processedPairs = processedPairs;
        this.averageMicros = averageMicros;
        this.peakMicros = peakMicros;
        this.deferredCount = deferredCount;
        this.deferredOverflows = deferredOverflows;
    }

    public List<String> lines() {
        return Collections.unmodifiableList(Arrays.asList(
                "Experimental tab refresh: " + (tabEnabled ? "ON" : "OFF"),
                "Experimental entity refresh configured: " + (entityEnabled ? "ON" : "OFF"),
                "Queued refreshes: " + pendingRefreshes + "; applied tab pairs: " + appliedEntries,
                "Tracked spawn snapshots: " + trackedSpawns + "; pending skin lookups: " + pendingSkins,
                "Counters do not prove successful client disguise rendering",
                "Urgent restorations waiting: " + urgentRestorations,
                "Dropped cosmetic refreshes: " + lostCosmetic
                        + "; displaced restorations: " + lostRestorations,
                "Entity refresh circuit: " + (entityEnabled && entityCircuitOpen ? "OPEN (tab only)" : "not tripped"),
                "Pending refresh retries: " + queuedRetries + "; exhausted: " + exhaustedRetries
                        + "; denied by capacity: " + rejectedRetries,
                "Skin sources in short failure cooldown: " + coolingDownSkins,
                "Refresh flushes: " + flushes + "; processed pairs: " + processedPairs,
                "Refresh flush wall us (average / peak): " + averageMicros + " / " + peakMicros,
                "Deferred cosmetic pairs: " + deferredCount + "; overflow: " + deferredOverflows
        ));
    }
}
