package ovh.paulem.krimson.listeners;

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
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.items.BlockItem;
import ovh.paulem.krimson.items.Items;

public class MigrationListener implements Listener {
    @EventHandler
    public void onPlayerJoinBlockItemMigration(PlayerJoinEvent event) {
        Krimson.getScheduler().runTaskAsynchronously(() -> {
            migrateInventory(event.getPlayer(), event.getPlayer().getInventory());
        });
    }

    @EventHandler
    public void onInventoryOpenMigration(InventoryOpenEvent event) {
        Krimson.getScheduler().runTaskAsynchronously(() -> {
            migrateInventory(event.getPlayer(), event.getInventory());
        });
    }

    private static void migrateInventory(HumanEntity player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if(item == null || item.getItemMeta() == null || !item.getItemMeta().hasItemModel()) continue;

            ItemMeta meta = item.getItemMeta();

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            @Nullable String identifier = pdc.get(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING);

            if(identifier == null) {
                continue;
            }

            NamespacedKey key = NamespacedKey.fromString(identifier);

            if (key == null) {
                Krimson.getInstance().getLogger().warning("Invalid item identifier: " + identifier + " for item: " + item.getType() + " in inventory of player: " + player.getName());
                continue;
            }

            BlockItem blockItem = Items.REGISTRY.getOrThrow(key);
            ItemStack toGive = blockItem.getItemStack();
            toGive.setAmount(item.getAmount());

            if(!toGive.equals(item)) {
                inventory.setItem(i, toGive);
                Krimson.getInstance().getLogger().info("Migrated item: " + item.getType() + " for player: " + player.getName() + " to his new reference.");
            }
        }

        Krimson.getInstance().getLogger().info("Migrated BlockItems in inventory of " + player.getName() + " to his new references.");
    }
}
