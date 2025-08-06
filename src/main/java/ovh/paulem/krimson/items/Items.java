package ovh.paulem.krimson.items;

import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.Blocks;
import ovh.paulem.krimson.blocks.custom.CustomBlock;
import ovh.paulem.krimson.registry.NewFrozenRegistry;

import java.util.function.Function;
import java.util.function.Supplier;

public class Items {
    public static final NewFrozenRegistry<CustomItem, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static final CustomBlockItem TEST = registerBlockItem(
            Blocks.TEST,
            (customBlock, player, placeLoc) -> {
                customBlock.clone().spawn(placeLoc);
            }
    );

    public static CustomBlockItem registerBlockItem(CustomBlock customBlock, TriConsumer<CustomBlock, @Nullable Player, Location> action) {
        NamespacedKey identifier = customBlock.getKey();

        return registerItem(identifier, key -> new CustomBlockItem(identifier, customBlock, action));
    }

    public static<T extends CustomItem> T registerItem(NamespacedKey identifier, Function<NamespacedKey, T> factory) {
        T item = factory.apply(identifier);
        REGISTRY.register(item);
        Krimson.getInstance().getLogger().info("Registered item: " + identifier.getKey());

        return (T) REGISTRY.getOrThrow(item.getKey());
    }

    public static void init() {
        Krimson.getInstance().getLogger().info("Registering items...");

        REGISTRY.freeze();
    }
}
