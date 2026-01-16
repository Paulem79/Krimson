package net.paulem.krimson.blocks;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.registry.NewFrozenRegistry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Blocks {
    public static final NewFrozenRegistry<CustomBlock, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static final InventoryCustomBlock TEST = register(
            "test_block",
            meta -> {
                meta.setDisplayName("§aTest Block");
                meta.setLore(List.of("§bThis is a test block", "§cfor inventory placement."));
            },
            key -> new InventoryCustomBlock(key, key, Material.OAK_WOOD, 3 * 9, "Inventaire de placement de test")
    );

    public static<T extends CustomBlock> T register(String key, @Nullable Consumer<ItemMeta> meta, Function<NamespacedKey, T> keyFunction) {
        NamespacedKey identifier = new NamespacedKey(Krimson.getInstance(), key);

        T customBlock = keyFunction.apply(identifier);
        if(meta != null) customBlock.setMeta(meta);
        REGISTRY.register(customBlock);
        Krimson.getInstance().getLogger().info("Registered block: " + key);

        return (T) REGISTRY.getOrThrow(customBlock.getKey());
    }

    public static void init() {
        Krimson.getInstance().getLogger().info("Registering blocks...");

        REGISTRY.freeze();
    }
}
