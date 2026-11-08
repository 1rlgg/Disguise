package dev.fyre.disguise.command;

import dev.fyre.disguise.api.Disguise;
import dev.fyre.disguise.api.DisguiseService;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Admin-only test commands. Modifies identity state, not world entity appearance. */
public final class DisguiseCommand implements CommandExecutor {
    private final DisguiseService disguises;

    public DisguiseCommand(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    private Player find(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null || !player.isOnline()) sender.sendMessage(ChatColor.RED + "Player must be online: " + name);
        return player;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fyredisguise.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            Player target = find(sender, args[1]);
            if (target != null) {
                try {
                    disguises.setDisguise(new Disguise(target.getUniqueId(), args[2]));
                    sender.sendMessage(ChatColor.GREEN + "Global alias updated. World appearance is unchanged.");
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
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set|clear|status <player> [alias]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " view <viewer> <player> <alias>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " unview|viewstatus <viewer> <player>");
    }
}
