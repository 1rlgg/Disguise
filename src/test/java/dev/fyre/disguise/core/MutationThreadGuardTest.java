package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class MutationThreadGuardTest {
    @Test public void rejectsWorkerMutationsWithoutChangingState() throws Exception {
        Thread owner = Thread.currentThread();
        DisguiseService service = new ObservableDisguiseService(new InMemoryDisguiseService(),
                transition -> {}, Collections::emptyList, () -> {
                    if (Thread.currentThread() != owner) throw new IllegalStateException("Wrong thread");
                });
        UUID subject = UUID.randomUUID();
        service.setDisguise(new Disguise(subject, "AllowedName"));
        AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try {
                service.setDisguise(new Disguise(subject, "WrongThread"));
            } catch (Throwable ex) {
                error.set(ex);
            }
        });
        worker.start();
        worker.join(2000);
        assertFalse(worker.isAlive());
        assertTrue(error.get() instanceof IllegalStateException);
        assertEquals("AllowedName", service.getDisguise(subject).get().getName());
    }

    @Test public void enforcesGuardForViewerOverridesAndClear() {
        DisguiseService service = new ObservableDisguiseService(new InMemoryDisguiseService(),
                transition -> {}, Collections::emptyList, () -> { throw new IllegalStateException("deny"); });
        UUID subject = UUID.randomUUID(), viewer = UUID.randomUUID();
        try {
            service.setDisguiseFor(viewer, new Disguise(subject, "SomeName"));
            fail("Expected guard");
        } catch (IllegalStateException expected) { }
        try {
            service.clear();
            fail("Expected guard");
        } catch (IllegalStateException expected) { }
    }
}
