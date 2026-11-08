package dev.fyre.disguise.render;

import dev.fyre.disguise.api.DisguiseService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Plans the smallest useful tab-list resynchronization after a join/respawn/world change.
 * This class never sends packets or touches world entity tracking.
 */
public final class RefreshTargetPlanner {
    private final DisguiseService disguises;

    public RefreshTargetPlanner(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    public Collection<RefreshQueue.Pair> around(UUID changedPlayer, Collection<UUID> onlinePlayers) {
        Objects.requireNonNull(changedPlayer, "changedPlayer");
        Objects.requireNonNull(onlinePlayers, "onlinePlayers");
        Set<RefreshQueue.Pair> targets = new LinkedHashSet<RefreshQueue.Pair>();
        for (UUID other : new ArrayList<UUID>(onlinePlayers)) {
            if (other == null || other.equals(changedPlayer)) continue;
            if (disguises.getDisguiseFor(changedPlayer, other).isPresent()) {
                targets.add(new RefreshQueue.Pair(changedPlayer, other));
            }
            if (disguises.getDisguiseFor(other, changedPlayer).isPresent()) {
                targets.add(new RefreshQueue.Pair(other, changedPlayer));
            }
        }
        return targets;
    }
}
