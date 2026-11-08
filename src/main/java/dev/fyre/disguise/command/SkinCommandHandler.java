package dev.fyre.disguise.command;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.skin.AsyncSkinService;
import dev.fyre.disguise.skin.SkinAssignment;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Asynchronous signed texture lookup with main-thread, stale-safe application. */
public final class SkinCommandHandler {
    private final JavaPlugin plugin;
    private final DisguiseService disguises;
    private final AsyncSkinService skins;
    private final SkinAssignment assignments;

    public SkinCommandHandler(JavaPlugin plugin, DisguiseService disguises, AsyncSkinService skins) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.disguises = Objects.requireNonNull(disguises, "disguises");
        this.skins = Objects.requireNonNull(skins, "skins");
        this.assignments = new SkinAssignment(disguises);
    }

    public void apply(CommandSender sender, Player subject, Player source) {
        apply(sender, subject, Objects.requireNonNull(source, "source").getUniqueId());
    }

    public void apply(CommandSender sender, Player subject, UUID source) {
        applyInternal(sender, null, subject, source);
    }

    public void applyForViewer(CommandSender sender, Player viewer, Player subject, Player source) {
        applyForViewer(sender, viewer, subject, Objects.requireNonNull(source, "source").getUniqueId());
    }

    public void applyForViewer(CommandSender sender, Player viewer, Player subject, UUID source) {
        applyInternal(sender, Objects.requireNonNull(viewer, "viewer").getUniqueId(), subject, source);
    }

    private void applyInternal(CommandSender sender, UUID viewerId, Player subject, UUID sourceId) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(sourceId, "sourceId");
        UUID subjectId = subject.getUniqueId();
        Disguise expected = viewerId == null
                ? disguises.getDisguise(subjectId).orElse(null)
                : disguises.getDisguiseFor(viewerId, subjectId).orElse(null);
        if (expected == null || (viewerId != null && disguises.getDisguise(subjectId).orElse(null) == expected)) {
            reply(sender, ChatColor.RED + "Set the corresponding alias before fetching a skin.");
            return;
        }
        reply(sender, ChatColor.YELLOW + "Looking up signed Mojang textures asynchronously...");
        skins.lookup(sourceId).whenComplete((skin, error) -> {
            if (!plugin.isEnabled()) return;
            try {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.isEnabled()) return;
                    Player currentSubject = Bukkit.getPlayer(subjectId);
                    Player currentViewer = viewerId == null ? null : Bukkit.getPlayer(viewerId);
                    if (currentSubject == null || !currentSubject.isOnline()
                            || (viewerId != null && (currentViewer == null || !currentViewer.isOnline()))) {
                        reply(sender, ChatColor.YELLOW + "Player disconnected; skin request discarded.");
                    } else if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Signed texture lookup failed for " + sourceId, error);
                        reply(sender, ChatColor.RED + "Mojang texture lookup failed.");
                    } else {
                        boolean applied = viewerId == null
                                ? assignments.applyIfCurrent(subjectId, expected, skin)
                                : assignments.applyForViewerIfCurrent(viewerId, subjectId, expected, skin);
                        reply(sender, applied
                                ? ChatColor.GREEN + "Skin stored; existing world entities do not refresh."
                                : ChatColor.YELLOW + "Alias changed; stale skin request discarded.");
                    }
                });
            } catch (IllegalStateException ex) {
                // Plugin may be shutting down.
            }
        });
    }

    private void reply(CommandSender sender, String message) {
        if (sender instanceof Player && !((Player) sender).isOnline()) return;
        sender.sendMessage(message);
    }
}
