package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;


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
