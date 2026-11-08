package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.render.IdentityPlanner;
import dev.fyre.disguise.render.IdentityRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Publishes minimal transitions after state changes.
 * Mutations must originate from the Bukkit primary thread.
 */
public final class ObservableDisguiseService implements DisguiseService {
    private final DisguiseService delegate;
    private final IdentityRenderer renderer;
    private final java.util.function.Supplier<Collection<UUID>> viewers;
    private final IdentityPlanner planner = new IdentityPlanner();

    public ObservableDisguiseService(DisguiseService delegate, IdentityRenderer renderer,
                                     java.util.function.Supplier<Collection<UUID>> viewers) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.viewers = Objects.requireNonNull(viewers, "viewers");
    }

    private void update(UUID subject, Runnable mutation) {
        Collection<UUID> recipients = new ArrayList<UUID>(viewers.get());
        java.util.Map<UUID, Disguise> previous = new java.util.HashMap<UUID, Disguise>();
        for (UUID viewer : recipients) previous.put(viewer, delegate.getDisguiseFor(viewer, subject).orElse(null));
        mutation.run();
        for (UUID viewer : recipients) {
            Disguise before = previous.get(viewer);
            Disguise after = delegate.getDisguiseFor(viewer, subject).orElse(null);
            planner.plan(viewer, subject, before, after).ifPresent(renderer::render);
        }
    }

    @Override public void setDisguise(Disguise disguise) {
        Objects.requireNonNull(disguise, "disguise");
        update(disguise.getPlayerId(), () -> delegate.setDisguise(disguise));
    }

    @Override public Optional<Disguise> getDisguise(UUID playerId) { return delegate.getDisguise(playerId); }

    @Override public boolean removeDisguise(UUID playerId) {
        boolean present = delegate.getDisguise(playerId).isPresent();
        if (present) update(playerId, () -> delegate.removeDisguise(playerId));
        return present;
    }

    @Override public void setDisguiseFor(UUID viewerId, Disguise disguise) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(disguise, "disguise");
        Disguise before = delegate.getDisguiseFor(viewerId, disguise.getPlayerId()).orElse(null);
        delegate.setDisguiseFor(viewerId, disguise);
        // Only send when this viewer is actually connected; registry accepts offline overrides.
        if (!viewers.get().contains(viewerId)) return;
        planner.plan(viewerId, disguise.getPlayerId(), before,
                delegate.getDisguiseFor(viewerId, disguise.getPlayerId()).orElse(null)).ifPresent(renderer::render);
    }

    @Override public Optional<Disguise> getDisguiseFor(UUID viewerId, UUID subjectId) {
        return delegate.getDisguiseFor(viewerId, subjectId);
    }

    @Override public boolean removeDisguiseFor(UUID viewerId, UUID subjectId) {
        Disguise before = delegate.getDisguiseFor(viewerId, subjectId).orElse(null);
        boolean removed = delegate.removeDisguiseFor(viewerId, subjectId);
        if (removed && viewers.get().contains(viewerId)) planner.plan(viewerId, subjectId, before,
                delegate.getDisguiseFor(viewerId, subjectId).orElse(null)).ifPresent(renderer::render);
        return removed;
    }

    @Override public void clearPlayer(UUID id) { delegate.clearPlayer(id); }
    @Override public void clear() { delegate.clear(); }
}
