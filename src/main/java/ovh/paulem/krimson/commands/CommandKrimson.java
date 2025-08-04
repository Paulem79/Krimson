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
    @Nullable
    public static BlockItem getItem(CommandSender sender, String[] args) {
        String itemKey = args[1];
        NamespacedKey key = NamespacedKey.fromString(itemKey);
        if (key == null) {
            sender.sendMessage("§cClé d'item invalide: " + itemKey);
            return null;
        }
        BlockItem item = Items.REGISTRY.getOrNull(key);

        if (item == null) {
            sender.sendMessage("§cItem non trouvé: " + itemKey);
            return null;
        }

        return item;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /krimson <give|fill|count|chunk>");
                return true;
            }
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "give" -> {
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /krimson give <item>");
                        return true;
                    }

                    BlockItem item = getItem(sender, args);

                    if (item == null) {
                        return true; // Item not found, error message already sent
                    }

                    player.getInventory().addItem(item.getItemStack());
                    sender.sendMessage("§aDon de 1x " + item.getKey());
                }
                case "fill" -> {
                    if (args.length < 5) {
                        sender.sendMessage("§cUsage: /krimson fill <item> <x> <y> <z>");
                        return true;
                    }

                    BlockItem item = getItem(sender, args);

                    if (item == null) {
                        return true; // Item not found, error message already sent
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

                    sender.sendMessage("§aZone de " + x * y * z + " blocs remplie avec " + item.getKey());
                }
                case "count" ->
                        sender.sendMessage("Il y a " + Krimson.customBlocks.getGlobalContainer().getAllBlocks().size() + " blocs custom chargés.\nTické:\n" +
                                " - Async: " + Krimson.customBlocks.getLastTickedCount().getOrDefault(true, 0) + "\n" +
                                " - Sync: " + Krimson.customBlocks.getLastTickedCount().getOrDefault(false, 0) + "\n" +
                                " - Total: " + Krimson.customBlocks.getLastTickedCount().values().stream().mapToInt(Integer::intValue).sum() + "\n");
                case "chunk" ->
                        sender.sendMessage(player.getLocation().getChunk().getPersistentDataContainer().getKeys().toString());
                default -> sender.sendMessage("§cSous-commande inconnue: " + subCommand);
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 2) {
            return Items.REGISTRY.keys().parallelStream().map(NamespacedKey::toString).toList();
        } else {
            return List.of("give", "fill", "count", "chunk");
        }
    }
}
