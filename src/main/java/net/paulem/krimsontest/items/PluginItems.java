package net.paulem.krimsontest.items;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.items.CustomArmorItem;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.CustomToolItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimsontest.blocks.PluginBlocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;

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

    public static final CustomToolItem mythril_AXE = Items.registerToolItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "mythril_axe"),
            Material.IRON_AXE,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/mythril_axe"), // modèle dans le resource pack
            List.of(CustomToolItem.AttributeModifierEntry.of(Attribute.BLOCK_BREAK_SPEED, 2.0, AttributeModifier.Operation.ADD_SCALAR)),
            new CustomToolItem.ToolProperties(4f, 1, List.of(
                    new CustomToolItem.ToolProperties.Rule(Material.OBSIDIAN, 8f, true)
            )),
            null,
            (tool, event) -> event.getPlayer().sendMessage("Interaction custom !"),
            null,
            null
    );

    private static final NamespacedKey mythril_EQUIP_ASSET = new NamespacedKey(KrimsonPlugin.getInstance(), "mythril");

    public static final CustomArmorItem mythril_HELMET = Items.registerArmorItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "mythril_helmet"),
            Material.IRON_HELMET,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/mythril_helmet"),
            EquipmentSlot.HEAD,
            mythril_EQUIP_ASSET,
            null,
            List.of(
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD),
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR_TOUGHNESS, 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
            ),
            null,
            null,
            null,
            null
    );

    public static final CustomArmorItem mythril_CHESTPLATE = Items.registerArmorItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "mythril_chestplate"),
            Material.IRON_CHESTPLATE,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/mythril_chestplate"),
            EquipmentSlot.CHEST,
            mythril_EQUIP_ASSET,
            null,
            List.of(
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR, 6.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST),
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
            ),
            null,
            null,
            null,
            null
    );

    public static final CustomArmorItem mythril_LEGGINGS = Items.registerArmorItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "mythril_leggings"),
            Material.IRON_LEGGINGS,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/mythril_leggings"),
            EquipmentSlot.LEGS,
            mythril_EQUIP_ASSET,
            null,
            List.of(
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR, 5.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS),
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
            ),
            null,
            null,
            null,
            null
    );

    public static final CustomArmorItem mythril_BOOTS = Items.registerArmorItem(
            new NamespacedKey(KrimsonPlugin.getInstance(), "mythril_boots"),
            Material.IRON_BOOTS,
            new NamespacedKey(KrimsonPlugin.getInstance(), "item/mythril_boots"),
            EquipmentSlot.FEET,
            mythril_EQUIP_ASSET,
            null,
            List.of(
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR, 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET),
                    new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR_TOUGHNESS, 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
            ),
            null,
            null,
            null,
            null
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items...");

        Items.REGISTRY.freeze();
    }
}
