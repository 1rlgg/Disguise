package dev.fyre.disguise.command;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import dev.fyre.disguise.skin.SkinSourceId;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Operator-only test controls; world entity appearance is not changed. */
public final class DisguiseCommand implements CommandExecutor {
    private final DisguiseService disguises;
    private final SkinCommandHandler skins;
    private final Supplier<List<String>> diagnostics;

    public DisguiseCommand(DisguiseService disguises) {
        this(disguises, null, null);
    }

    public DisguiseCommand(DisguiseService disguises, SkinCommandHandler skins) {
        this(disguises, skins, null);
    }

    public DisguiseCommand(DisguiseService disguises, SkinCommandHandler skins,
                          Supplier<List<String>> diagnostics) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
        this.skins = skins;
        this.diagnostics = diagnostics;
    }

    private Player find(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null || !player.isOnline()) sender.sendMessage(ChatColor.RED + "Player must be online: " + name);
        return player;
    }

    private UUID skinSource(CommandSender sender, String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null && online.isOnline()) return online.getUniqueId();
        java.util.Optional<UUID> uuid = SkinSourceId.parse(input);
        if (uuid.isPresent()) return uuid.get();
        sender.sendMessage(ChatColor.RED + "Source must be an online player name or complete UUID.");
        return null;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fyredisguise.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("stats")) {
            if (diagnostics == null) {
                sender.sendMessage(ChatColor.RED + "Diagnostics unavailable.");
            } else {
                for (String line : diagnostics.get()) sender.sendMessage(ChatColor.YELLOW + line);
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            if (skins == null) { sender.sendMessage(ChatColor.RED + "Skin lookup unavailable."); return true; }
            Player target = find(sender, args[1]);
            UUID source = skinSource(sender, args[2]);
            if (target != null && source != null) skins.apply(sender, target, source);
            return true;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("viewskin")) {
            if (skins == null) { sender.sendMessage(ChatColor.RED + "Skin lookup unavailable."); return true; }
            Player viewer = find(sender, args[1]);
            Player target = find(sender, args[2]);
            UUID source = skinSource(sender, args[3]);
            if (viewer != null && target != null && source != null) skins.applyForViewer(sender, viewer, target, source);
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            Player target = find(sender, args[1]);
            if (target != null) {
                try {
                    disguises.setDisguise(new Disguise(target.getUniqueId(), args[2]));
                    sender.sendMessage(ChatColor.GREEN + "Global alias updated. World appearance unchanged.");
                } catch (IllegalArgumentException ex) { invalidAlias(sender); }
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            Player target = find(sender, args[1]);
            if (target != null) sender.sendMessage(disguises.removeDisguise(target.getUniqueId())
                    ? ChatColor.GREEN + "Global alias removed." : ChatColor.YELLOW + "No global alias.");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            Player target = find(sender, args[1]);
            if (target != null) sender.sendMessage(ChatColor.YELLOW + "Global alias: "
                    + disguises.getDisguise(target.getUniqueId()).map(Disguise::getName).orElse("(none)"));
            return true;
        }
        if (args.length >= 3 && (args[0].equalsIgnoreCase("view")
                || args[0].equalsIgnoreCase("unview") || args[0].equalsIgnoreCase("viewstatus"))) {
            String action = args[0].toLowerCase(java.util.Locale.ROOT);
            if (action.equals("view") && args.length != 4) { usage(sender, label); return true; }
            if (!action.equals("view") && args.length != 3) { usage(sender, label); return true; }
            Player viewer = find(sender, args[1]);
            Player target = find(sender, args[2]);
            if (viewer == null || target == null) return true;
            if (action.equals("view")) {
                try {
                    disguises.setDisguiseFor(viewer.getUniqueId(), new Disguise(target.getUniqueId(), args[3]));
                    sender.sendMessage(ChatColor.GREEN + "Viewer override updated. World appearance unchanged.");
                } catch (IllegalArgumentException ex) { invalidAlias(sender); }
            } else if (action.equals("unview")) {
                sender.sendMessage(disguises.removeDisguiseFor(viewer.getUniqueId(), target.getUniqueId())
                        ? ChatColor.GREEN + "Viewer override removed." : ChatColor.YELLOW + "No viewer override.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Effective alias for " + viewer.getName() + ": "
                        + disguises.getDisguiseFor(viewer.getUniqueId(), target.getUniqueId())
                          .map(Disguise::getName).orElse("(none)"));
            }
            return true;
        }
        usage(sender, label);
        return true;
    }

    private static void invalidAlias(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Alias must be 3-16 letters, digits or underscores.");
    }

    private static void usage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " stats");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set|clear|status <player> [alias]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " view <viewer> <player> <alias>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " unview|viewstatus <viewer> <player>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " skin <online-target> <online-source|uuid>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " viewskin <viewer> <target> <online-source|uuid>");
    }
}
