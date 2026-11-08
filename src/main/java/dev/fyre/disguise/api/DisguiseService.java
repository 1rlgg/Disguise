package dev.fyre.disguise.api;

import java.util.Optional;
import java.util.UUID;

/** Stores desired disguise state; client-side rendering is not yet implemented. */
public interface DisguiseService {
    void setDisguise(Disguise disguise);
    Optional<Disguise> getDisguise(UUID playerId);
    boolean removeDisguise(UUID playerId);
    void clear();
}
