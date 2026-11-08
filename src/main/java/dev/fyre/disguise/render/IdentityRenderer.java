package dev.fyre.disguise.render;

/**
 * Platform-specific render backend. Called on the Bukkit primary thread only.
 * Implementations must tolerate a missing/disconnected viewer or subject.
 */
public interface IdentityRenderer {
    void render(IdentityTransition transition);
}
