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

/**
 * Permission must be checked by caller. This command never performs HTTP on the
 * main thread and will not apply stale results after alias changes/disconnect.
 */
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
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(source, "source");
        UUID subjectId = subject.getUniqueId();
        Disguise expected = disguises.getDisguise(subjectId).orElse(null);
        if (expected == null) {
            sender.sendMessage(ChatColor.RED + "Set a global alias before fetching a skin.");
            return;
        }
        sender.sendMessage(ChatColor.YELLOW + "Looking up Mojang textures asynchronously...");
        skins.lookup(source.getUniqueId()).whenComplete((skin, error) -> {
            if (!plugin.isEnabled()) return;
            try {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.isEnabled()) return;
                    Player current = Bukkit.getPlayer(subjectId);
                    if (current == null || !current.isOnline()) {
                        reply(sender, ChatColor.YELLOW + "Target disconnected; skin request discarded.");
                    } else if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Skin lookup failed for source UUID " + source.getUniqueId(), error);
                        reply(sender, ChatColor.RED + "Mojang texture lookup failed.");
                    } else if (assignments.applyIfCurrent(subjectId, expected, skin)) {
                        reply(sender, ChatColor.GREEN + "Signed skin data applied to alias state. "
                                + "World entity appearance still requires a separate refresh.");
                    } else {
                        reply(sender, ChatColor.YELLOW + "Alias changed; stale skin request discarded.");
                    }
                });
            } catch (IllegalStateException ex) {
                // Plugin may have been disabled between scheduling and completion.
            }
        });
    }

    private void reply(CommandSender sender, String message) {
        if (sender instanceof Player && !((Player) sender).isOnline()) return;
        sender.sendMessage(message);
    }
}
