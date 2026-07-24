package net.paulem.krimsontest.commands;

import net.kyori.adventure.bossbar.BossBar;
import net.paulem.krimsontest.ui.PluginUIs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UICommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage("Usage: /ui <type> [action]");
            sender.sendMessage("Types: font");
            sender.sendMessage("Actions: show, hide");
            return true;
        }

        String type = args[0].toLowerCase();
        String action = args.length > 1 ? args[1].toLowerCase() : "show";

        if (type.equals("font")) {
            if (action.equals("show")) {
                PluginUIs.FONT_MANA_BAR.displayBossBar(player, 0.75f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
                sender.sendMessage("Showing font-based mana bar!");
            } else if (action.equals("hide")) {
                PluginUIs.FONT_MANA_BAR.hide(player);
                sender.sendMessage("Hiding font-based mana bar!");
            }
        } else {
            sender.sendMessage("Unknown UI type: " + type);
            return false;
        }

        return true;
    }
}