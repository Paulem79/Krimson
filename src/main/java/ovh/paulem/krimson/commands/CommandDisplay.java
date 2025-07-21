package ovh.paulem.krimson.commands;

import org.bukkit.Material;
import org.bukkit.command.TabExecutor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.blocks.CustomBlock;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import ovh.paulem.krimson.blocks.LightBlock;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandDisplay implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /display <light|inventory|basic>");
                return true;
            }
            String subCommand = args[0].toLowerCase();
            Location blockLoc = player.getLocation().getBlock().getLocation();

            switch (subCommand) {
                case "light":
                    new LightBlock(15).spawn(blockLoc);
                    sender.sendMessage("§aBloc lumineux affiché.");
                    break;
                case "inventory":
                    new InventoryCustomBlock(Material.AMETHYST_BLOCK, new ItemStack(Material.BONE_BLOCK), 54, "Test inventory").spawn(blockLoc);
                    sender.sendMessage("§aBloc d'inventaire affiché.");
                    break;
                case "basic":
                    new CustomBlock(Material.BONE_BLOCK, new ItemStack(Material.ACACIA_PLANKS))
                            .spawn(blockLoc);
                    sender.sendMessage("§aBloc basique affiché.");
                    break;
                default:
                    sender.sendMessage("§cSous-commande inconnue: " + subCommand);
                    break;
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of("light", "inventory", "basic");
    }
}
