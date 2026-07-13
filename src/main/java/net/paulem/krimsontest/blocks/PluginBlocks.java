package net.paulem.krimsontest.blocks;

import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.Material;

import java.util.List;

public class PluginBlocks {
    public static final InventoryCustomBlock TEST = Blocks.register(
            "test_block",
            meta -> {
                meta.setDisplayName("§aTest Block");
                meta.setLore(List.of("§bThis is a test block", "§cfor inventory placement."));
            },
            key -> new InventoryCustomBlock(key, key, Material.OAK_WOOD, 3 * 9, "Inventaire de placement de test")
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering blocks...");

        Blocks.REGISTRY.freeze();
    }
}
