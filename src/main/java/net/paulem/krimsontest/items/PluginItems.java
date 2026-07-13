package net.paulem.krimsontest.items;

import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimsontest.blocks.PluginBlocks;

public class PluginItems {
    public static final CustomBlockItem TEST = Items.registerBlockItem(
            PluginBlocks.TEST,
            (customBlock, player, placeLoc) ->
                customBlock.copyOf().spawn(placeLoc)
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items...");

        Items.REGISTRY.freeze();
    }
}
