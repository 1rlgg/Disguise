package dev.fyre.disguise.protocol;

import dev.fyre.disguise.protocol.entity.EntityRefreshPolicy;
import dev.fyre.disguise.protocol.packet.PlayerInfoPacketPolicy;

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

public final class PlayerInfoRewriter {
    private final DisguiseService disguises;

    public PlayerInfoRewriter(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    public List<PlayerInfoData> rewrite(UUID viewerId, List<PlayerInfoData> original) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(original, "original");

        if (disguises instanceof DisguiseSubjectCatalog
                && !((DisguiseSubjectCatalog) disguises).hasAnySubjects()) return original;
        List<PlayerInfoData> result = null;

        Set<UUID> knownSubjects = null;
        for (int index = 0; index < original.size(); index++) {
            PlayerInfoData info = original.get(index);
            if (info == null || info.getProfile() == null) continue;
            UUID subjectId = info.getProfile().getUUID();
            if (subjectId == null || !PlayerInfoPacketPolicy.mayRewrite(viewerId, subjectId)) continue;
            Optional<Disguise> wanted = disguises.getDisguiseFor(viewerId, subjectId);
            if (!wanted.isPresent()) continue;
            Disguise disguise = wanted.get();

            if (disguises instanceof DisguiseSubjectCatalog) {
                if (knownSubjects == null)
                    knownSubjects = ((DisguiseSubjectCatalog) disguises).subjectIds();
                if (EntityRefreshPolicy.conflicts(disguises, viewerId, subjectId,
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

            WrappedChatComponent label = WrappedChatComponent.fromText(disguise.getName());
            result.set(index, new PlayerInfoData(profile, info.getLatency(), info.getGameMode(), label));
        }
        return result == null ? original : result;
    }
}
