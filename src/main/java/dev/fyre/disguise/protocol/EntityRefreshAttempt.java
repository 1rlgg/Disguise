package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

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

                failure.addSuppressed(new EntityCircuitOpenedException());
            }

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

    public static final class EntityCircuitOpenedException extends RuntimeException {
        public EntityCircuitOpenedException() { super("Experimental entity refresh circuit opened"); }
    }
}
