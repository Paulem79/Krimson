package net.paulem.krimson.blocks;

import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.registry.NewFrozenRegistry;

import java.util.function.Consumer;
import java.util.function.Function;

public class Blocks {
    public static final NewFrozenRegistry<CustomBlock, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static<T extends CustomBlock> T register(String key, @Nullable Consumer<ItemMeta> meta, Function<NamespacedKey, T> keyFunction) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);

        T customBlock = keyFunction.apply(identifier);
        if(meta != null) customBlock.setMeta(meta);
        REGISTRY.register(customBlock);
        KrimsonPlugin.getInstance().getLogger().info("Registered block: " + key);

        return (T) REGISTRY.getOrThrow(customBlock.getKey());
    }

    private Blocks() {
        /* This utility class should not be instantiated */
    }
}
