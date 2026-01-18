package net.paulem.krimson.items;

import net.paulem.krimson.common.KrimsonPlugin;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.registry.NewFrozenRegistry;

import java.util.function.Function;

public class Items {
    public static final NewFrozenRegistry<CustomItem, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static final CustomBlockItem TEST = registerBlockItem(
            Blocks.TEST,
            (customBlock, player, placeLoc) ->
                customBlock.copyOf().spawn(placeLoc)
    );

    public static CustomBlockItem registerBlockItem(CustomBlock customBlock, TriConsumer<CustomBlock, @Nullable Player, Location> action) {
        NamespacedKey identifier = customBlock.getKey();

        return registerItem(identifier, key -> new CustomBlockItem(identifier, customBlock, action));
    }

    public static<T extends CustomItem> T registerItem(NamespacedKey identifier, Function<NamespacedKey, T> factory) {
        T item = factory.apply(identifier);
        REGISTRY.register(item);
        KrimsonPlugin.getInstance().getLogger().info("Registered item: " + identifier.getKey());

        return (T) REGISTRY.getOrThrow(item.getKey());
    }

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items...");

        REGISTRY.freeze();
    }

    private Items() {}
}
