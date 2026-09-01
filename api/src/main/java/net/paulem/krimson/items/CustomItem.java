package net.paulem.krimson.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import net.paulem.krimson.registry.RegistryKey;

public abstract class CustomItem implements RegistryKey<NamespacedKey> {
    private final NamespacedKey key;

    public CustomItem(NamespacedKey key) {
        this.key = key;
    }

    public abstract ItemStack getItemStack();

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
