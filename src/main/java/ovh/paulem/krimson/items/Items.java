package ovh.paulem.krimson.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.blocks.InventoryCustomBlock;
import ovh.paulem.krimson.function.TriConsumer;
import ovh.paulem.krimson.registry.NewFrozenRegistry;

import java.util.List;
import java.util.function.Consumer;

public class Items {
    public static final NewFrozenRegistry<BlockItem, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static final BlockItem TEST = register(
            "test_block",
            Material.OAK_WOOD,
            meta -> {
                meta.setDisplayName("§cTest Item");
                meta.setLore(List.of("§eThis is a test item", "§7for the Krimson plugin."));
            },
            (blockItem, player, placeLoc) -> {
                InventoryCustomBlock customBlock = new InventoryCustomBlock(blockItem.getKey(), blockItem.getMaterial(), blockItem.getItemStack(), 3 * 9, "Inventaire de placement de test");
                customBlock.spawn(placeLoc);
            }
    );

    public static BlockItem register(String key, Material material, Consumer<ItemMeta> meta, TriConsumer<BlockItem, @Nullable Player, Location> action) {
        NamespacedKey identifier = new NamespacedKey(Krimson.getInstance(), key);

        BlockItem blockItem = new BlockItem(identifier, material, meta, action);
        REGISTRY.register(blockItem);
        Krimson.getInstance().getLogger().info("Registered item: " + key);

        return REGISTRY.getOrThrow(blockItem.getKey());
    }

    public static void init() {
        Krimson.getInstance().getLogger().info("Registering items...");

        REGISTRY.freeze();
    }
}
