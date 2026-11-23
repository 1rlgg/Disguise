package dev.fyre.disguise.core;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.render.identity.IdentityTransition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ObservableDisguiseService implements DisguiseService {
    private final DisguiseService delegate;
    private final Consumer<IdentityTransition> renderer;
    private final Supplier<Collection<UUID>> viewers;
    private final Runnable mutationGuard;

    public ObservableDisguiseService(DisguiseService delegate, Consumer<IdentityTransition> renderer,
                                     Supplier<Collection<UUID>> viewers) {
        this(delegate, renderer, viewers, () -> { });
    }

    public ObservableDisguiseService(DisguiseService delegate, Consumer<IdentityTransition> renderer,
                                     Supplier<Collection<UUID>> viewers,
                                     Runnable mutationGuard) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.viewers = Objects.requireNonNull(viewers, "viewers");
        this.mutationGuard = Objects.requireNonNull(mutationGuard, "mutationGuard");
    }

    private void refreshAliasNeighbours(UUID viewer, UUID changedSubject,
                                        Disguise before, Disguise after) {
        if (!(delegate instanceof DisguiseSubjectCatalog)) return;
        String oldName = before == null ? null : before.getName();
        String newName = after == null ? null : after.getName();
        if (oldName == null && newName == null) return;
        if (oldName != null && newName != null && oldName.equalsIgnoreCase(newName)) return;
        for (UUID other : ((DisguiseSubjectCatalog) delegate).subjectIds()) {
            if (other.equals(changedSubject) || other.equals(viewer)) continue;
            Disguise active = delegate.getDisguiseFor(viewer, other).orElse(null);
            if (active == null) continue;
            String name = active.getName();
            if ((oldName != null && name.equalsIgnoreCase(oldName))
                    || (newName != null && name.equalsIgnoreCase(newName))) {
                renderer.accept(new IdentityTransition(viewer, other, active, active));
            }
        }
    }

    private void update(UUID subject, Runnable mutation) {
        Collection<UUID> recipients = new ArrayList<UUID>(viewers.get());
        Map<UUID, Disguise> previous = new HashMap<UUID, Disguise>();
        for (UUID viewer : recipients) previous.put(viewer, delegate.getDisguiseFor(viewer, subject).orElse(null));
        mutation.run();
        for (UUID viewer : recipients) {
            Disguise before = previous.get(viewer);
            Disguise after = delegate.getDisguiseFor(viewer, subject).orElse(null);
            Optional<IdentityTransition> transition =
                    IdentityTransition.plan(viewer, subject, before, after);
            if (transition.isPresent()) {
                renderer.accept(transition.get());
                refreshAliasNeighbours(viewer, subject, before, after);
            }
        }
    }

    @Override public void setDisguise(Disguise disguise) {
        mutationGuard.run();
        Objects.requireNonNull(disguise, "disguise");
        update(disguise.getPlayerId(), () -> delegate.setDisguise(disguise));
    }

    @Override public Optional<Disguise> getDisguise(UUID playerId) { return delegate.getDisguise(playerId); }

    @Override public boolean removeDisguise(UUID playerId) {
        mutationGuard.run();
        boolean present = delegate.getDisguise(playerId).isPresent();
        if (present) update(playerId, () -> delegate.removeDisguise(playerId));
        return present;
    }

    @Override public void setDisguiseFor(UUID viewerId, Disguise disguise) {
        mutationGuard.run();
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(disguise, "disguise");
        Disguise before = delegate.getDisguiseFor(viewerId, disguise.getPlayerId()).orElse(null);
        delegate.setDisguiseFor(viewerId, disguise);
        if (!viewers.get().contains(viewerId)) return;
        Disguise after = delegate.getDisguiseFor(viewerId, disguise.getPlayerId()).orElse(null);
        Optional<IdentityTransition> transition = IdentityTransition.plan(viewerId, disguise.getPlayerId(), before, after);
        if (transition.isPresent()) {
            renderer.accept(transition.get());
            refreshAliasNeighbours(viewerId, disguise.getPlayerId(), before, after);
        }
    }

    @Override public Optional<Disguise> getDisguiseFor(UUID viewerId, UUID subjectId) {
        return delegate.getDisguiseFor(viewerId, subjectId);
    }

    @Override public Optional<Disguise> getViewerOverride(UUID viewerId, UUID subjectId) {
        return delegate.getViewerOverride(viewerId, subjectId);
    }

    @Override public boolean removeDisguiseFor(UUID viewerId, UUID subjectId) {
        mutationGuard.run();
        Disguise before = delegate.getDisguiseFor(viewerId, subjectId).orElse(null);
        boolean removed = delegate.removeDisguiseFor(viewerId, subjectId);
        if (removed && viewers.get().contains(viewerId)) {
            Disguise after = delegate.getDisguiseFor(viewerId, subjectId).orElse(null);
            Optional<IdentityTransition> transition = IdentityTransition.plan(viewerId, subjectId, before, after);
            if (transition.isPresent()) {
                renderer.accept(transition.get());
                refreshAliasNeighbours(viewerId, subjectId, before, after);
            }
        }
        return removed;
    }

    @Override public void clearPlayer(UUID id) {
        mutationGuard.run();
        Objects.requireNonNull(id, "id");

        Collection<UUID> online = new ArrayList<UUID>(viewers.get());
        Map<UUID, Disguise> beforeSubject = new LinkedHashMap<UUID, Disguise>();
        for (UUID viewer : online) {
            beforeSubject.put(viewer, delegate.getDisguiseFor(viewer, id).orElse(null));
        }
        Map<UUID, Disguise> beforeViewer = new LinkedHashMap<UUID, Disguise>();
        if (online.contains(id) && delegate instanceof DisguiseSubjectCatalog) {
            for (UUID subject : ((DisguiseSubjectCatalog) delegate).subjectIds()) {
                if (!subject.equals(id)) {
                    beforeViewer.put(subject, delegate.getDisguiseFor(id, subject).orElse(null));
                }
            }
        }
        delegate.clearPlayer(id);
        for (java.util.Map.Entry<UUID, Disguise> entry : beforeSubject.entrySet()) {
            Disguise after = delegate.getDisguiseFor(entry.getKey(), id).orElse(null);
            Optional<IdentityTransition> transition =
                    IdentityTransition.plan(entry.getKey(), id, entry.getValue(), after);
            if (transition.isPresent()) {
                renderer.accept(transition.get());
                refreshAliasNeighbours(entry.getKey(), id, entry.getValue(), after);
            }
        }
        for (java.util.Map.Entry<UUID, Disguise> entry : beforeViewer.entrySet()) {
            Disguise after = delegate.getDisguiseFor(id, entry.getKey()).orElse(null);
            Optional<IdentityTransition> transition =
                    IdentityTransition.plan(id, entry.getKey(), entry.getValue(), after);
            if (transition.isPresent()) {
                renderer.accept(transition.get());
                refreshAliasNeighbours(id, entry.getKey(), entry.getValue(), after);
            }
        }
    }

    @Override public void clear() {
        mutationGuard.run();
        if (!(delegate instanceof DisguiseSubjectCatalog)) {
            delegate.clear();
            return;
        }
        Collection<UUID> recipients = new ArrayList<UUID>(viewers.get());
        Map<UUID, Map<UUID, Disguise>> before =
                new LinkedHashMap<UUID, Map<UUID, Disguise>>();
        for (UUID subject : ((DisguiseSubjectCatalog) delegate).subjectIds()) {
            Map<UUID, Disguise> identities = new LinkedHashMap<UUID, Disguise>();
            for (UUID viewer : recipients) {
                Disguise previous = delegate.getDisguiseFor(viewer, subject).orElse(null);
                if (previous != null) identities.put(viewer, previous);
            }
            if (!identities.isEmpty()) before.put(subject, identities);
        }
        delegate.clear();
        for (java.util.Map.Entry<UUID, Map<UUID, Disguise>> subject : before.entrySet()) {
            for (java.util.Map.Entry<UUID, Disguise> identity : subject.getValue().entrySet()) {
                IdentityTransition.plan(identity.getKey(), subject.getKey(), identity.getValue(), null)
                        .ifPresent(renderer::accept);
            }
        }
    }
}
