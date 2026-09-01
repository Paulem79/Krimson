package net.paulem.krimsontest.items;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.CustomToolItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimsontest.blocks.PluginBlocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.List;

public class PluginItems {
    private PluginItems() {
        /* This utility class should not be instantiated */
    }

    public static final CustomBlockItem TEST = Items.registerBlockItem(
            PluginBlocks.TEST,
            (customBlock, player, placeLoc) ->
                customBlock.copyOf().spawn(placeLoc)
    );

    public static final CustomBlockItem NOTE_TEST = Items.registerBlockItem(
            PluginBlocks.NOTE_TEST,
            (customBlock, player, placeLoc) ->
                customBlock.copyOf().spawn(placeLoc)
    );

    public static final CustomToolItem COPPER_AXE = Items.registerToolItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "copper_axe"),
            Material.IRON_AXE,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/copper_axe"), // modèle dans le resource pack
            List.of(CustomToolItem.AttributeModifierEntry.of(Attribute.BLOCK_BREAK_SPEED, 2.0, AttributeModifier.Operation.ADD_SCALAR)),
            new CustomToolItem.ToolProperties(4f, 1, List.of(
                    new CustomToolItem.ToolProperties.Rule(Material.OBSIDIAN, 8f, true)
            )),
            null,
            (tool, event) -> event.getPlayer().sendMessage("Interaction custom !"),
            null,
            null
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items...");

        Items.REGISTRY.freeze();
    }
}
