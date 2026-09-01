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

    /**
     * Sets the item model to the given key verbatim (no {@code "block/"} prefix), for custom tools/armor whose
     * model lives under a different path (e.g. {@code item/...}) in the resource pack.
     */
    public static ItemStack getWithRawItemModel(ItemStack item, NamespacedKey modelKey) {
        if (item == null) {
            return null;
        }
        ItemStack newItem = item.clone();
        ItemMeta meta = newItem.getItemMeta();

        if (meta != null) {
            meta.setItemModel(modelKey);

            newItem.setItemMeta(meta);
        }

        return newItem;
    }
}
