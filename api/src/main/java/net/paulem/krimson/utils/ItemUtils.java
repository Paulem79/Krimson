package net.paulem.krimson.utils;

import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    public static ItemStack getWithItemModel(ItemStack item, NamespacedKey modelPath) {
        if (item == null) {
            return null;
        }
        ItemStack newItem = item.clone();
        ItemMeta meta = newItem.getItemMeta();

        if (meta != null) {
            NamespacedKey newKey = new NamespacedKey(KrimsonPlugin.getInstance(), "block/" + modelPath.getKey());
            meta.setItemModel(newKey);

            newItem.setItemMeta(meta);
        }

        return newItem;
    }
}
