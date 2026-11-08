package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Separates experimental entity transport health from tab-only delivery.
 * Tab fallback errors must not trip the world-entity circuit breaker.
 */
public final class EntityRefreshAttempt {
    @FunctionalInterface public interface WorldSender {
        boolean refresh() throws InvocationTargetException;
    }
    @FunctionalInterface public interface TabSender {
        void refresh() throws InvocationTargetException;
    }

    private final EntityRefreshFailureGate gate;

    public EntityRefreshAttempt(EntityRefreshFailureGate gate) {
        this.gate = Objects.requireNonNull(gate, "gate");
    }

    /**
     * Use the tab fallback when world refresh is unavailable or skipped.
     * Failure is propagated for renderer retries; only entity failures affect
     * the entity circuit breaker.
     */
    public boolean deliver(WorldSender world, TabSender tab) throws InvocationTargetException {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(tab, "tab");
        if (!gate.enabled()) {
            tab.refresh();
            return false;
        }
        boolean changed;
        try {
            changed = world.refresh();
        } catch (InvocationTargetException | RuntimeException failure) {
            boolean disabled = gate.failure();
            if (disabled) {
                // Only world errors trip the entity breaker, even if the
                // fallback also fails. Mark the transition for one log entry.
                failure.addSuppressed(new EntityCircuitOpenedException());
            }
            // Best-effort tab delivery on EVERY world failure. Waiting for
            // the third failure can leave the viewer's tab entry stale.
            // Keep the original world exception to drive bounded retries.
            try {
                tab.refresh();
            } catch (InvocationTargetException | RuntimeException fallbackFailure) {
                if (fallbackFailure != failure) failure.addSuppressed(fallbackFailure);
            }
            throw failure;
        }
        if (changed) gate.success();
        else tab.refresh();
        return false;
    }

    /** Marker attached only when this operation trips the entity circuit. */
    public static final class EntityCircuitOpenedException extends RuntimeException {
        public EntityCircuitOpenedException() { super("Experimental entity refresh circuit opened"); }
    }
}
