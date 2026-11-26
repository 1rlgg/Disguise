package gg.rl1.disguise.render;

import gg.rl1.disguise.api.DisguiseService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class RefreshTargetPlanner {
    private final DisguiseService disguises;

    public RefreshTargetPlanner(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    public Collection<RefreshQueue.Pair> around(UUID changedPlayer, Collection<UUID> onlinePlayers) {
        return around(changedPlayer, onlinePlayers, pair -> false);
    }

    public Collection<RefreshQueue.Pair> around(UUID changedPlayer, Collection<UUID> onlinePlayers,
                                                Predicate<RefreshQueue.Pair> previouslyApplied) {
        Objects.requireNonNull(previouslyApplied, "previouslyApplied");
        Objects.requireNonNull(changedPlayer, "changedPlayer");
        Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        Set<RefreshQueue.Pair> targets = new LinkedHashSet<RefreshQueue.Pair>();
        for (UUID other : onlinePlayers) {
            if (other == null || other.equals(changedPlayer)) continue;
            RefreshQueue.Pair asViewer = new RefreshQueue.Pair(changedPlayer, other);
            RefreshQueue.Pair asSubject = new RefreshQueue.Pair(other, changedPlayer);
            if (disguises.getDisguiseFor(changedPlayer, other).isPresent()
                    || previouslyApplied.test(asViewer)) targets.add(asViewer);
            if (disguises.getDisguiseFor(other, changedPlayer).isPresent()
                    || previouslyApplied.test(asSubject)) targets.add(asSubject);
        }
        return targets;
    }
}
