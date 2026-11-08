package dev.fyre.disguise.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import java.util.List;
import org.bukkit.plugin.Plugin;

/**
 * Rewrites future player-info additions only. Existing client entries require
 * a separately tested refresh flow; do not synthesize entity-spawn packets here.
 */
public final class PlayerInfoPacketListener extends PacketAdapter {
    private final PlayerInfoRewriter rewriter;

    public PlayerInfoPacketListener(Plugin plugin, PlayerInfoRewriter rewriter) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.PLAYER_INFO);
        this.rewriter = rewriter;
    }

    @Override public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (packet.getPlayerInfoAction().read(0) != EnumWrappers.PlayerInfoAction.ADD_PLAYER) return;
        List<PlayerInfoData> existing = packet.getPlayerInfoDataLists().read(0);
        if (existing == null || existing.isEmpty()) return;
        List<PlayerInfoData> updated = rewriter.rewrite(event.getPlayer().getUniqueId(), existing);
        if (updated != existing) packet.getPlayerInfoDataLists().write(0, updated);
    }
}
