package dev.fyre.disguise.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import dev.fyre.disguise.api.DisguiseService;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Player;

/**
 * Experimental per-viewer tab-list synchronization. No world entity or
 * nametag refresh is performed; caller must use Bukkit primary thread.
 */
public final class PlayerInfoTabRefresh {
    private final ProtocolManager protocol;
    private final PlayerInfoRewriter rewriter;

    public PlayerInfoTabRefresh(ProtocolManager protocol, DisguiseService service) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.rewriter = new PlayerInfoRewriter(Objects.requireNonNull(service, "service"));
    }

    public void refresh(Player viewer, Player subject) throws InvocationTargetException {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(subject, "subject");
        if (viewer.equals(subject) || !viewer.isOnline() || !subject.isOnline() || !viewer.canSee(subject)) return;

        WrappedGameProfile realProfile = WrappedGameProfile.fromPlayer(subject);
        PlayerInfoData original = new PlayerInfoData(realProfile, 0,
                EnumWrappers.NativeGameMode.valueOf(subject.getGameMode().name()),
                WrappedChatComponent.fromText(subject.getPlayerListName() == null
                        ? subject.getName() : subject.getPlayerListName()));
        List<PlayerInfoData> input = Collections.singletonList(original);
        List<PlayerInfoData> output = rewriter.rewrite(viewer.getUniqueId(), input);

        PacketContainer remove = makePacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, input);
        PacketContainer add = makePacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, output);

        // Never hide/show Bukkit players: doing so alters entity tracking and vanish state.
        protocol.sendServerPacket(viewer, remove, false);
        try {
            protocol.sendServerPacket(viewer, add, false);
        } catch (InvocationTargetException | RuntimeException failure) {
            // Best-effort recovery: avoid leaving a viewer without a tab entry if
            // the custom profile could not be serialized or delivered.
            try {
                protocol.sendServerPacket(viewer,
                        makePacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, input), false);
            } catch (InvocationTargetException | RuntimeException restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            throw failure;
        }
    }

    private PacketContainer makePacket(EnumWrappers.PlayerInfoAction action, List<PlayerInfoData> data) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, action);
        packet.getPlayerInfoDataLists().write(0, data);
        return packet;
    }
}
