package gg.rl1.disguise.command;

import gg.rl1.disguise.api.Disguise;
import gg.rl1.disguise.api.DisguiseService;
import gg.rl1.disguise.skin.AsyncSkinService;
import gg.rl1.disguise.skin.SkinAssignment;
import gg.rl1.disguise.skin.SkinRequestTracker;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinCommandHandler {
    private final JavaPlugin plugin;
    private final DisguiseService disguises;
    private final AsyncSkinService skins;
    private final SkinAssignment assignments;
    private final SkinRequestTracker requests = new SkinRequestTracker(512);

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
                : disguises.getViewerOverride(viewerId, subjectId).orElse(null);
        if (expected == null) {
            reply(sender, ChatColor.RED + "Set the corresponding alias before fetching a skin.");
            return;
        }
        final long token;
        try {
            token = requests.begin(viewerId, subjectId);
        } catch (IllegalStateException overloaded) {
            reply(sender, ChatColor.RED + "Too many active skin requests; try again later.");
            return;
        }
        reply(sender, ChatColor.YELLOW + "Looking up signed Mojang textures asynchronously...");
        java.util.concurrent.CompletableFuture<gg.rl1.disguise.api.SkinProperty> lookup;
        try {
            lookup = skins.lookup(sourceId);
        } catch (RuntimeException error) {
            requests.finish(viewerId, subjectId, token);
            plugin.getLogger().log(Level.WARNING, "Could not start signed texture lookup", error);
            reply(sender, ChatColor.RED + "Mojang texture lookup failed.");
            return;
        }
        java.util.concurrent.CompletableFuture<?> callback = lookup.whenComplete((skin, error) -> {
            if (!requests.isCurrent(viewerId, subjectId, token)) return;
            if (!plugin.isEnabled()) {
                requests.finish(viewerId, subjectId, token);
                return;
            }
            Runnable completion = () -> completeLookup(
                    sender, viewerId, subjectId, sourceId, expected, skin, error, token);
            if (Bukkit.isPrimaryThread()) {
                completion.run();
                return;
            }
            try {
                Bukkit.getScheduler().runTask(plugin, completion);
            } catch (RuntimeException ex) {
                requests.finish(viewerId, subjectId, token);
                if (plugin.isEnabled()) {
                    plugin.getLogger().log(Level.WARNING,
                            "Could not schedule the signed texture callback", ex);
                }
            }
        });
        requests.attach(viewerId, subjectId, token, callback);
    }

    private void completeLookup(CommandSender sender, UUID viewerId, UUID subjectId, UUID sourceId,
                                Disguise expected, gg.rl1.disguise.api.SkinProperty skin,
                                Throwable error, long token) {
        if (!plugin.isEnabled()) {
            requests.finish(viewerId, subjectId, token);
            return;
        }
        if (!requests.isCurrent(viewerId, subjectId, token)) return;
        try {
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
                        ? ChatColor.GREEN + "Skin stored; experimental client refresh may update appearance."
                        : ChatColor.YELLOW + "Alias changed; stale skin request discarded.");
            }
        } finally {
            requests.finish(viewerId, subjectId, token);
        }
    }

    public void cancel(UUID viewerId, UUID subjectId) { requests.cancel(viewerId, subjectId); }

    public void forgetPlayer(UUID playerId) { requests.forgetPlayer(playerId); }
    public void clearPending() { requests.clear(); }

    private void reply(CommandSender sender, String message) {
        if (sender instanceof Player && !((Player) sender).isOnline()) return;
        sender.sendMessage(message);
    }
}
