package net.paulem.krimson.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import net.paulem.krimson.registry.RegistryKey;

public class CustomItem implements RegistryKey<NamespacedKey> {
    private final NamespacedKey key;

    public CustomItem(NamespacedKey key) {
        this.key = key;
    }

    public ItemStack getItemStack() {
        // This method should be overridden by subclasses to return the actual ItemStack
        throw new UnsupportedOperationException("This method should be overridden in subclasses (for now)");
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
