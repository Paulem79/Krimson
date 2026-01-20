package net.paulem.krimson.listeners;

import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.Items;

public class MigrationListener implements Listener {
    private static void migrateInventory(HumanEntity player, Inventory inventory) {
        boolean hasMigrated = false;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) continue;

            ItemMeta meta = item.getItemMeta();

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            @Nullable String identifier = pdc.get(new NamespacedKey(KrimsonPlugin.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

            if (identifier == null) {
                continue;
            }

            NamespacedKey key = NamespacedKey.fromString(identifier);

            if (key == null) {
                KrimsonPlugin.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " in inventory of player: " + player.getName());
                continue;
            }

            CustomBlockItem customBlockItem = (CustomBlockItem) Items.REGISTRY.getOrThrow(key);

            ItemStack toGive = customBlockItem.getCustomBlock().getItemDisplayStack();
            toGive.setAmount(item.getAmount());

            if (!toGive.equals(item)) {
                inventory.setItem(i, toGive);

                hasMigrated = true;
                KrimsonPlugin.getInstance().getLogger().info("Migrated item: " + item.getType() + " for player: " + player.getName() + " to his new reference.");
            }
        }

        if (hasMigrated) {
            KrimsonPlugin.getInstance().getLogger().info("Migrated BlockItems in inventory of " + player.getName() + " to his new references.");
        }
    }

    @EventHandler
    public void onPlayerJoinBlockItemMigration(PlayerJoinEvent event) {
        KrimsonPlugin.getScheduler().runTaskAsynchronously(() ->
            migrateInventory(event.getPlayer(), event.getPlayer().getInventory())
        );
    }

    @EventHandler
    public void onInventoryOpenMigration(InventoryOpenEvent event) {
        KrimsonPlugin.getScheduler().runTaskAsynchronously(() ->
            migrateInventory(event.getPlayer(), event.getInventory())
        );
    }
}
