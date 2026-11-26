package gg.rl1.disguise.protocol.packet;

import gg.rl1.disguise.protocol.PacketRewriteBreaker;


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

public final class PlayerInfoPacketListener extends PacketAdapter {
    private final PlayerInfoRewriter rewriter;
    private final PacketRewriteBreaker breaker = new PacketRewriteBreaker(5);
    private final AtomicBoolean firstErrorLogged = new AtomicBoolean();

    public PlayerInfoPacketListener(Plugin plugin, PlayerInfoRewriter rewriter) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Server.PLAYER_INFO);
        this.rewriter = rewriter;
    }

    @Override public void onPacketSending(PacketEvent event) {
        if (breaker.isOpen() || event.isCancelled()) return;
        try {
            PacketContainer packet = event.getPacket();
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            boolean add = action == EnumWrappers.PlayerInfoAction.ADD_PLAYER;
            boolean display = action == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME;
            if (!add && !display) return;
            List<PlayerInfoData> existing = packet.getPlayerInfoDataLists().read(0);
            if (!PlayerInfoRewriter.mayRewrite(event.isCancelled(), add, display,
                    existing != null && !existing.isEmpty())) return;
            List<PlayerInfoData> updated = rewriter.rewrite(event.getPlayer().getUniqueId(), existing);

            if (updated != existing) {
                packet.getPlayerInfoDataLists().write(0, updated);
            }
            breaker.recordSuccess();
        } catch (RuntimeException ex) {
            boolean nowDisabled = breaker.recordFailure();
            if (firstErrorLogged.compareAndSet(false, true)) {
                getPlugin().getLogger().log(Level.WARNING,
                        "Player-info alias rewriting failed; leaving original packets unchanged", ex);
            }
            if (nowDisabled) {
                getPlugin().getLogger().severe(
                        "Disabling player-info rewriting after repeated failures; restart after fixing the issue");
            }
        }
    }
}
