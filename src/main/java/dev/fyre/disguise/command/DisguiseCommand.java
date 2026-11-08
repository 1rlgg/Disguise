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

/**
 * Controlled test entry point for aliases. Does not implement full world-entity disguises.
 * Only server operators / users with fyredisguise.admin may use this command.
 */
public final class DisguiseCommand implements CommandExecutor {
    private final DisguiseService disguises;

    public DisguiseCommand(DisguiseService disguises) {
        this.disguises = Objects.requireNonNull(disguises, "disguises");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fyredisguise.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Target player must be online.");
                return true;
            }
            try {
                disguises.setDisguise(new Disguise(target.getUniqueId(), args[2]));
                sender.sendMessage(ChatColor.GREEN + "Alias state updated for " + target.getName()
                        + ". World entity appearance is not yet changed.");
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Alias must contain 3-16 letters, digits or underscores.");
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Target player must be online.");
                return true;
            }
            boolean changed = disguises.removeDisguise(target.getUniqueId());
            sender.sendMessage(changed ? ChatColor.GREEN + "Alias state cleared."
                    : ChatColor.YELLOW + "No global alias is set.");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Target player must be online.");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " global alias: "
                    + disguises.getDisguise(target.getUniqueId()).map(Disguise::getName).orElse("(none)"));
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <online-player> <alias>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " clear <online-player>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status <online-player>");
        return true;
    }
}
