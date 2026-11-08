package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/** Best-effort atomic-like tab entry replacement for ambiguous network failures. */
public final class TabRefreshTransaction {
    private TabRefreshTransaction() { }

    public interface Sender<T> {
        void send(T packet) throws InvocationTargetException;
    }

    public static <T> void execute(T remove, T aliasAdd, T originalAdd,
                                   Sender<T> sender) throws InvocationTargetException {
        Objects.requireNonNull(remove, "remove");
        Objects.requireNonNull(aliasAdd, "aliasAdd");
        Objects.requireNonNull(originalAdd, "originalAdd");
        Objects.requireNonNull(sender, "sender");
        try {
            sender.send(remove);
            sender.send(aliasAdd);
        } catch (InvocationTargetException | RuntimeException failure) {
            // Packet sends are ambiguous. The alias may already have reached
            // the client even if its transport call threw. Clear that slot
            // first, then restore the original profile regardless of whether
            // cleanup itself fails. Preserve every secondary error.
            attempt(sender, remove, failure);
            attempt(sender, originalAdd, failure);
            throw failure;
        }
    }

    private static <T> void attempt(Sender<T> sender, T packet, Throwable primary) {
        try {
            sender.send(packet);
        } catch (InvocationTargetException | RuntimeException recovery) {
            if (recovery != primary) primary.addSuppressed(recovery);
        }
    }
}
