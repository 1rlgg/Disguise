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
     * Returns true exactly when this attempt disables the entity backend.
     * If the world refresh is unsafe/skipped, use the tab fallback instead.
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
                // The caller needs to log the circuit transition; preserve the
                // original failure to make retry and recovery behavior explicit.
                failure.addSuppressed(new EntityCircuitOpenedException());
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
