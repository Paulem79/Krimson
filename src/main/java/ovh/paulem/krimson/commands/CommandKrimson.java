package ovh.paulem.krimson.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;

import java.util.List;

public class CommandKrimson implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /krimson <give>");
                return true;
            }
            String subCommand = args[0].toLowerCase();

            if (args.length < 2) {
                sender.sendMessage("§cUsage: /krimson give <item>");
                return true;
            }

            String itemKey = args[1];
            NamespacedKey key = NamespacedKey.fromString(itemKey);
            if (key == null) {
                sender.sendMessage("§cClé d'item invalide: " + itemKey);
                return true;
            }
            BlockItem item = Items.REGISTRY.getOrNull(key);

            if (item == null) {
                sender.sendMessage("§cItem non trouvé: " + itemKey);
                return true;
            }

            switch (subCommand) {
                case "give":
                    player.getInventory().addItem(item.getItemStack());
                    sender.sendMessage("§aDon de 1x " + itemKey);
                    break;
                case "fill":
                    if (args.length < 5) {
                        sender.sendMessage("§cUsage: /krimson fill <item> <x> <y> <z>");
                        return true;
                    }
                    int x, y, z;
                    try {
                        x = Integer.parseInt(args[2]);
                        y = Integer.parseInt(args[3]);
                        z = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cLes coordonnées doivent être des entiers.");
                        return true;
                    }

                    var loc = player.getLocation().getBlock().getLocation();
                    for (int dx = 0; dx <= x; dx++) {
                        for (int dy = 0; dy <= y; dy++) {
                            for (int dz = 0; dz <= z; dz++) {
                                var blockLoc = loc.clone().add(dx, dy, dz);
                                item.getAction().accept(item, player, blockLoc);
                            }
                        }
                    }

                    sender.sendMessage("§aZone de " + x*y*z + " blocs remplie avec " + itemKey);
                    break;
                case "count":
                    sender.sendMessage("Il y a " + Krimson.customBlocks.size() + " blocs custom chargés.");
                    break;
                default:
                    sender.sendMessage("§cSous-commande inconnue: " + subCommand);
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 2) {
            return Items.REGISTRY.keys().parallelStream().map(NamespacedKey::toString).toList();
        } else {
            return List.of("give", "fill", "count");
        }
    }
}
