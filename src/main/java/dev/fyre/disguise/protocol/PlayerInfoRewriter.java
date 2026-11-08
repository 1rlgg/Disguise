package dev.fyre.disguise.protocol;

import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.api.SkinProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Pure profile conversion. Do not mutate original profile or incoming list. */
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
            if (disguise.getSkin().isPresent()) {
                SkinProperty skin = disguise.getSkin().get();
                // A signed property is expected here; Base64 validation alone is not signature verification.
                if (skin.isSigned()) profile.getProperties().put("textures",
                        new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
            } else {
                // Keep original textures when no custom skin was requested.
                for (WrappedSignedProperty texture : info.getProfile().getProperties().get("textures")) {
                    profile.getProperties().put("textures", texture);
                }
            }
            result.set(index, new PlayerInfoData(profile, info.getLatency(),
                    info.getGameMode(), info.getDisplayName()));
        }
        return result == null ? original : result;
    }
}
