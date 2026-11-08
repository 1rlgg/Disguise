package dev.fyre.disguise.protocol;

import com.comphenix.protocol.wrappers.PlayerInfoData;
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
 * Replaces only the selected visible identity, never mutating the original packet list,
 * and retaining unrelated player profile properties.
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
            Optional<SkinProperty> desiredSkin = disguise.getSkin();
            boolean hasSignedSkin = desiredSkin.isPresent() && desiredSkin.get().isSigned();
            for (Map.Entry<String, WrappedSignedProperty> entry : info.getProfile().getProperties().entries()) {
                if (!hasSignedSkin || !"textures".equals(entry.getKey())) {
                    profile.getProperties().put(entry.getKey(), entry.getValue());
                }
            }
            if (hasSignedSkin) {
                SkinProperty skin = desiredSkin.get();
                profile.getProperties().put("textures",
                        new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
            }
            result.set(index, new PlayerInfoData(profile, info.getLatency(),
                    info.getGameMode(), info.getDisplayName()));
        }
        return result == null ? original : result;
    }
}
