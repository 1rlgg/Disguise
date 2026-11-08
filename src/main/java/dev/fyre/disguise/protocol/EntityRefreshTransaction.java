package dev.fyre.disguise.protocol;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

/**
 * Testable sequencing for the experimental client-side player respawn.
 * The caller prepares all packets before invoking this class.
 *
 * A send may have reached the client even when the transport reports failure.
 * On any error after starting the destroy, replay the original snapshot and
 * current equipment as a best-effort recovery; do not mutate Bukkit entities.
 */
public final class EntityRefreshTransaction {
    private EntityRefreshTransaction() { }

    public interface Sender<T> {
        void send(T packet) throws InvocationTargetException;
    }

    public static <T> void execute(T destroy, T respawn, T originalSpawn, T head,
                                   List<T> equipment, Sender<T> transport)
            throws InvocationTargetException {
        Objects.requireNonNull(destroy, "destroy");
        Objects.requireNonNull(respawn, "respawn");
        Objects.requireNonNull(originalSpawn, "originalSpawn");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(equipment, "equipment");
        Objects.requireNonNull(transport, "transport");
        try {
            transport.send(destroy);
            transport.send(respawn);
            transport.send(head);
            for (T item : equipment) transport.send(item);
        } catch (InvocationTargetException | RuntimeException failure) {
            // A send failure is ambiguous: the client may have received the
            // packet. Best effort, always restore a coherent original spawn.
            // A recovery step may also throw. Still attempt every subsequent
            // step; preserving all failures on the original exception improves
            // diagnosis and gives the client the best chance to regain state.
            trySend(transport, destroy, failure);
            trySend(transport, originalSpawn, failure);
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
