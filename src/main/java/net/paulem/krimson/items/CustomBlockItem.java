package net.paulem.krimson.items;

import lombok.Getter;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.blocks.custom.CustomBlock;

public class CustomBlockItem extends CustomItem {
    @Getter
    public final CustomBlock customBlock;
    @Getter
    private final TriConsumer<CustomBlock, @Nullable Player, Location> action;

    public CustomBlockItem(NamespacedKey key, CustomBlock customBlock, TriConsumer<CustomBlock, @Nullable Player, Location> action) {
        super(key);

        this.customBlock = customBlock;
        this.action = action;
    }

    @Override
    public ItemStack getItemStack() {
        return getCustomBlock().getItemDisplayStack();
    }
}
