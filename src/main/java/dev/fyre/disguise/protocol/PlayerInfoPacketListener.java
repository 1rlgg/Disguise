package dev.fyre.disguise.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

/** Fail-open PLAYER_INFO listener: a broken alias may never block a server packet. */
public final class PlayerInfoPacketListener extends PacketAdapter {
    private final PlayerInfoRewriter rewriter;
    private final AtomicBoolean errorLogged = new AtomicBoolean();

    public PlayerInfoPacketListener(Plugin plugin, PlayerInfoRewriter rewriter) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.PLAYER_INFO);
        this.rewriter = rewriter;
    }

    @Override public void onPacketSending(PacketEvent event) {
        try {
            PacketContainer packet = event.getPacket();
            if (packet.getPlayerInfoAction().read(0) != EnumWrappers.PlayerInfoAction.ADD_PLAYER) return;
            List<PlayerInfoData> existing = packet.getPlayerInfoDataLists().read(0);
            if (existing == null || existing.isEmpty()) return;
            List<PlayerInfoData> updated = rewriter.rewrite(event.getPlayer().getUniqueId(), existing);
            if (updated != existing) packet.getPlayerInfoDataLists().write(0, updated);
        } catch (RuntimeException ex) {
            if (errorLogged.compareAndSet(false, true)) {
                getPlugin().getLogger().log(Level.WARNING,
                        "Player-info rewriting failed; leaving the original packet unchanged (first occurrence)", ex);
            }
        }
    }
}
