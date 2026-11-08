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
            try {
                sender.send(originalAdd);
            } catch (InvocationTargetException | RuntimeException recovery) {
                if (recovery != failure) failure.addSuppressed(recovery);
            }
            throw failure;
        }
    }
}
