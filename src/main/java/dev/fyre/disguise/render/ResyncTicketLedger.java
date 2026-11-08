package dev.fyre.disguise.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Main-thread-only tickets for delayed join/respawn/world resync tasks.
 * An older delayed callback must never operate on a newer player session.
 */
public final class ResyncTicketLedger {
    private final Map<UUID, Long> latest = new HashMap<UUID, Long>();
    private long sequence;

    public long schedule(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        long ticket = ++sequence;
        latest.put(playerId, ticket);
        return ticket;
    }

    /** Consumes a ticket so accidental repeated callbacks are harmless. */
    public boolean consume(UUID playerId, long ticket) {
        Objects.requireNonNull(playerId, "playerId");
        Long current = latest.get(playerId);
        if (current == null || current.longValue() != ticket) return false;
        latest.remove(playerId);
        return true;
    }

    public void forget(UUID playerId) {
        latest.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public int pendingCount() { return latest.size(); }
    public void clear() { latest.clear(); }
}
