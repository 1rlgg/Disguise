package gg.rl1.disguise.protocol.packet;

import gg.rl1.disguise.protocol.entity.EntityRefreshPolicy;

import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.DisguiseService;
import gg.rl1.disguise.core.DisguiseSubjectCatalog;
import gg.rl1.disguise.api.SkinProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
                && !((DisguiseSubjectCatalog) disguises).hasAnySubjectsFor(viewerId)) return original;
        List<PlayerInfoData> result = null;

        Set<UUID> knownSubjects = null;
        Map<String, Integer> aliasCounts = null;
        for (int index = 0; index < original.size(); index++) {
            PlayerInfoData info = original.get(index);
            if (info == null || info.getProfile() == null) continue;
            UUID subjectId = info.getProfile().getUUID();
            if (subjectId == null || !mayRewrite(viewerId, subjectId)) continue;
            Optional<Disguise> wanted = disguises.getDisguiseFor(viewerId, subjectId);
            if (!wanted.isPresent()) continue;
            Disguise disguise = wanted.get();

            if (disguises instanceof DisguiseSubjectCatalog) {
                if (knownSubjects == null)
                    knownSubjects = ((DisguiseSubjectCatalog) disguises).subjectIds();
                boolean collision;
                if (original.size() > 4) {
                    if (aliasCounts == null) aliasCounts = aliasCounts(viewerId, knownSubjects);
                    Integer count = aliasCounts.get(disguise.getName().toLowerCase(Locale.ROOT));
                    collision = count != null && count.intValue() > 1;
                } else {
                    collision = EntityRefreshPolicy.conflicts(disguises, viewerId, subjectId,
                            disguise.getName(), knownSubjects);
                }
                if (collision) continue;
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
    private Map<String, Integer> aliasCounts(UUID viewerId, Set<UUID> subjects) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (UUID subject : subjects) {
            if (subject == null || subject.equals(viewerId)) continue;
            Disguise disguise = disguises.getDisguiseFor(viewerId, subject).orElse(null);
            if (disguise == null) continue;
            String alias = disguise.getName().toLowerCase(Locale.ROOT);
            Integer prior = counts.get(alias);
            counts.put(alias, prior == null ? 1 : prior + 1);
        }
        return counts;
    }

    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean hasEntries) {

        return mayRewrite(packetCancelled, addPlayerAction, false, hasEntries);
    }

    public static boolean mayRewrite(boolean packetCancelled, boolean addPlayerAction,
                                     boolean updateDisplayNameAction, boolean hasEntries) {
        return !packetCancelled && (addPlayerAction || updateDisplayNameAction) && hasEntries;
    }
    public static boolean mayRewrite(java.util.UUID viewer, java.util.UUID subject) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        java.util.Objects.requireNonNull(subject, "subject");
        return !viewer.equals(subject);
    }
}
