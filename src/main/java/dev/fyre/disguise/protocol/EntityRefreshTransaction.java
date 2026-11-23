package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;


public final class EntityRefreshTransaction {
    private EntityRefreshTransaction() { }

    public interface Sender<T> {
        void send(T packet) throws InvocationTargetException;
    }

    public static <T> void execute(T destroy, T respawn, T recoverySpawn, T head,
                                   List<T> equipment, Sender<T> transport)
            throws InvocationTargetException {
        Objects.requireNonNull(destroy, "destroy");
        Objects.requireNonNull(respawn, "respawn");
        Objects.requireNonNull(recoverySpawn, "recoverySpawn");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(transport, "transport");
        try {
            transport.send(destroy);
            transport.send(respawn);
            transport.send(head);
            for (T item : equipment) transport.send(item);
        } catch (InvocationTargetException | RuntimeException failure) {


            trySend(transport, destroy, failure);
            trySend(transport, recoverySpawn, failure);
            trySend(transport, head, failure);
            for (T item : equipment) trySend(transport, item, failure);
            throw failure;
        }
    }

    private static <T> void trySend(Sender<T> transport, T packet, Throwable original) {
        try {
            transport.send(packet);
        } catch (InvocationTargetException | RuntimeException error) {
            if (error != original) original.addSuppressed(error);
        }
    }
}
