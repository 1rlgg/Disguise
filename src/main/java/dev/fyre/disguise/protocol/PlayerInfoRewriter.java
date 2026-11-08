package dev.fyre.disguise.protocol;

import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.api.SkinProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        List<PlayerInfoData> result = null;
        for (int index = 0; index < original.size(); index++) {
            PlayerInfoData info = original.get(index);
            if (info == null || info.getProfile() == null) continue;
            UUID subjectId = info.getProfile().getUUID();
            if (subjectId == null) continue;
            Optional<Disguise> wanted = disguises.getDisguiseFor(viewerId, subjectId);
            if (!wanted.isPresent()) continue;
            Disguise disguise = wanted.get();
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
