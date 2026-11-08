package dev.fyre.disguise.protocol;

import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.core.DisguiseSubjectCatalog;
import dev.fyre.disguise.api.SkinProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Applies per-viewer visible player-info profile and display name. A non-null
 * old display name can expose the undisguised username, so the alias is used
 * in both places. Original packets and unrelated profile properties remain intact.
 */
public final class PlayerInfoRewriter {
    private final DisguiseService disguises;

    public PlayerInfoRewriter(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    public List<PlayerInfoData> rewrite(UUID viewerId, List<PlayerInfoData> original) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(original, "original");
        // Most packets on a normal server need no profile rewrite. Avoid
        // allocating/copying profile structures when the registry is empty.
        if (disguises instanceof DisguiseSubjectCatalog
                && !((DisguiseSubjectCatalog) disguises).hasAnySubjects()) return original;
        List<PlayerInfoData> result = null;
        // Freeze the subject catalog once per outgoing packet. Without this,
        // each entry in a batch could allocate a new subject set and observe
        // a different collision set during concurrent disguise updates.
        Set<UUID> knownSubjects = null;
        for (int index = 0; index < original.size(); index++) {
            PlayerInfoData info = original.get(index);
            if (info == null || info.getProfile() == null) continue;
            UUID subjectId = info.getProfile().getUUID();
            if (subjectId == null || !PlayerInfoRewritePolicy.mayRewrite(viewerId, subjectId)) continue;
            Optional<Disguise> wanted = disguises.getDisguiseFor(viewerId, subjectId);
            if (!wanted.isPresent()) continue;
            Disguise disguise = wanted.get();
            // Packet listeners may run off the Bukkit thread. Use only the
            // synchronized disguise catalog, never Bukkit online-player
            // lookups, to reject conflicting desired names for this viewer.
            // On conflict leave the vanilla entry unchanged.
            if (disguises instanceof DisguiseSubjectCatalog) {
                if (knownSubjects == null)
                    knownSubjects = ((DisguiseSubjectCatalog) disguises).subjectIds();
                if (ViewerAliasCollisionPolicy.conflicts(disguises, viewerId, subjectId,
                        disguise.getName(), knownSubjects)) continue;
            }
            if (result == null) result = new ArrayList<PlayerInfoData>(original);
            WrappedGameProfile profile = new WrappedGameProfile(subjectId, disguise.getName());
            Optional<SkinProperty> requestedSkin = disguise.getSkin();
            boolean signedSkin = requestedSkin.isPresent() && requestedSkin.get().isSigned();
            for (Map.Entry<String, WrappedSignedProperty> entry : info.getProfile().getProperties().entries()) {
                if (!signedSkin || !"textures".equals(entry.getKey())) {
                    profile.getProperties().put(entry.getKey(), entry.getValue());
                }
            }
            if (signedSkin) {
                SkinProperty skin = requestedSkin.get();
                profile.getProperties().put("textures",
                        new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
            }
            // The 1.8 player-info display-name field overrides the profile name.
            // Explicitly use the alias to avoid retaining a real-name list label.
            WrappedChatComponent label = WrappedChatComponent.fromText(disguise.getName());
            result.set(index, new PlayerInfoData(profile, info.getLatency(), info.getGameMode(), label));
        }
        return result == null ? original : result;
    }
}
