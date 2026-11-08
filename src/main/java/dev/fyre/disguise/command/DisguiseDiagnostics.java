package dev.fyre.disguise.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable, unprivileged rendering statistics; command access remains admin-only. */
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

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins) {
        this(tabEnabled, entityEnabled, pendingRefreshes, appliedEntries, trackedSpawns, pendingSkins,
                0, 0, 0, false);
    }

    public DisguiseDiagnostics(boolean tabEnabled, boolean entityEnabled,
                              int pendingRefreshes, int appliedEntries,
                              int trackedSpawns, int pendingSkins,
                              int urgentRestorations, long lostCosmetic,
                              long lostRestorations, boolean entityCircuitOpen) {
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
                "Entity refresh circuit: " + (entityEnabled && entityCircuitOpen ? "OPEN (tab only)" : "not tripped")
        ));
    }
}
