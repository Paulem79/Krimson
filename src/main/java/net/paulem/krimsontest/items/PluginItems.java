package net.paulem.krimsontest.items;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.items.CustomArmorItem;
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.CustomToolItem;
import net.paulem.krimson.items.Items;
import net.paulem.krimson.items.PlainItem;
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

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static NamespacedKey key(String key) {
        return new NamespacedKey(KrimsonPlugin.getInstance(), key);
    }

    private static NamespacedKey itemModel(String key) {
        return new NamespacedKey(KrimsonPlugin.getInstance(), "item/" + key);
    }

    private static PlainItem plainItem(String name, Material baseMaterial) {
        return Items.registerPlainItem(key(name), baseMaterial, itemModel(name), null);
    }

    private static CustomToolItem miningTool(
            String name, Material baseMaterial, float miningSpeedBonus, float defaultMiningSpeed
    ) {
        return Items.registerToolItem(
                key(name),
                baseMaterial,
                itemModel(name),
                List.of(CustomToolItem.AttributeModifierEntry.of(Attribute.BLOCK_BREAK_SPEED, miningSpeedBonus, AttributeModifier.Operation.ADD_SCALAR)),
                new CustomToolItem.ToolProperties(defaultMiningSpeed, 1, List.of()),
                null, null, null, null
        );
    }

    private static CustomToolItem weapon(String name, Material baseMaterial, double attackDamageBonus) {
        return Items.registerToolItem(
                key(name),
                baseMaterial,
                itemModel(name),
                List.of(CustomToolItem.AttributeModifierEntry.of(Attribute.ATTACK_DAMAGE, attackDamageBonus, AttributeModifier.Operation.ADD_NUMBER)),
                null,
                null, null, null, null
        );
    }

    private static CustomToolItem shears(String name, Material baseMaterial) {
        return Items.registerToolItem(
                key(name),
                baseMaterial,
                itemModel(name),
                List.of(),
                null,
                null, null, null, null
        );
    }

    private static CustomArmorItem armor(
            String name, Material baseMaterial, EquipmentSlot slot, EquipmentSlotGroup slotGroup,
            NamespacedKey equipAsset, double armorBonus, double toughnessBonus
    ) {
        return Items.registerArmorItem(
                key(name),
                baseMaterial,
                itemModel(name),
                slot,
                equipAsset,
                null,
                List.of(
                        new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR, armorBonus, AttributeModifier.Operation.ADD_NUMBER, slotGroup),
                        new CustomToolItem.AttributeModifierEntry(Attribute.ARMOR_TOUGHNESS, toughnessBonus, AttributeModifier.Operation.ADD_NUMBER, slotGroup)
                ),
                null, null, null, null
        );
    }

    // -----------------------------------------------------------------
    // Blocks -> Items
    // -----------------------------------------------------------------

    public static final CustomBlockItem TEST = Items.registerBlockItem(
            PluginBlocks.TEST,
            (customBlock, player, placeLoc) -> customBlock.copyOf().spawn(placeLoc)
    );

    public static final CustomBlockItem TIN_ORE = Items.registerBlockItem(PluginBlocks.TIN_ORE, PluginItems::place);
    public static final CustomBlockItem DEEPSLATE_TIN_ORE = Items.registerBlockItem(PluginBlocks.DEEPSLATE_TIN_ORE, PluginItems::place);
    public static final CustomBlockItem TIN_BLOCK = Items.registerBlockItem(PluginBlocks.TIN_BLOCK, PluginItems::place);
    public static final CustomBlockItem RAW_TIN_BLOCK = Items.registerBlockItem(PluginBlocks.RAW_TIN_BLOCK, PluginItems::place);

    public static final CustomBlockItem MYTHRIL_ORE = Items.registerBlockItem(PluginBlocks.MYTHRIL_ORE, PluginItems::place);
    public static final CustomBlockItem DEEPSLATE_MYTHRIL_ORE = Items.registerBlockItem(PluginBlocks.DEEPSLATE_MYTHRIL_ORE, PluginItems::place);
    public static final CustomBlockItem MYTHRIL_BLOCK = Items.registerBlockItem(PluginBlocks.MYTHRIL_BLOCK, PluginItems::place);
    public static final CustomBlockItem RAW_MYTHRIL_BLOCK = Items.registerBlockItem(PluginBlocks.RAW_MYTHRIL_BLOCK, PluginItems::place);

    public static final CustomBlockItem ADAMANTIUM_ORE = Items.registerBlockItem(PluginBlocks.ADAMANTIUM_ORE, PluginItems::place);
    public static final CustomBlockItem DEEPSLATE_ADAMANTIUM_ORE = Items.registerBlockItem(PluginBlocks.DEEPSLATE_ADAMANTIUM_ORE, PluginItems::place);
    public static final CustomBlockItem ADAMANTIUM_BLOCK = Items.registerBlockItem(PluginBlocks.ADAMANTIUM_BLOCK, PluginItems::place);
    public static final CustomBlockItem RAW_ADAMANTIUM_BLOCK = Items.registerBlockItem(PluginBlocks.RAW_ADAMANTIUM_BLOCK, PluginItems::place);

    public static final CustomBlockItem ONYX_ORE = Items.registerBlockItem(PluginBlocks.ONYX_ORE, PluginItems::place);
    public static final CustomBlockItem ONYX_BLOCK = Items.registerBlockItem(PluginBlocks.ONYX_BLOCK, PluginItems::place);

    private static void place(net.paulem.krimson.blocks.custom.CustomBlock customBlock, org.bukkit.entity.Player player, org.bukkit.Location placeLoc) {
        customBlock.copyOf().spawn(placeLoc);
    }

    // -----------------------------------------------------------------
    // Raw materials / ingots / gems
    // -----------------------------------------------------------------

    public static final PlainItem RAW_TIN = plainItem("raw_tin", Material.RAW_IRON);
    public static final PlainItem TIN_INGOT = plainItem("tin_ingot", Material.IRON_INGOT);

    public static final PlainItem COPPER_INGOT = plainItem("copper_ingot", Material.IRON_INGOT);

    public static final PlainItem RAW_MYTHRIL = plainItem("raw_mythril", Material.RAW_IRON);
    public static final PlainItem MYTHRIL_INGOT = plainItem("mythril_ingot", Material.IRON_INGOT);

    public static final PlainItem RAW_ADAMANTIUM = plainItem("raw_adamantium", Material.RAW_IRON);
    public static final PlainItem ADAMANTIUM_INGOT = plainItem("adamantium_ingot", Material.NETHERITE_INGOT);

    public static final PlainItem ONYX_GEM = plainItem("onyx_gem", Material.EMERALD);

    // -----------------------------------------------------------------
    // Tin tools & armor (weakest tier: stone-ish)
    // -----------------------------------------------------------------

    public static final CustomToolItem TIN_PICKAXE = miningTool("tin_pickaxe", Material.STONE_PICKAXE, 0.5f, 2f);
    public static final CustomToolItem TIN_AXE = weapon("tin_axe", Material.STONE_AXE, 1);
    public static final CustomToolItem TIN_SHOVEL = miningTool("tin_shovel", Material.STONE_SHOVEL, 0.5f, 2f);
    public static final CustomToolItem TIN_HOE = miningTool("tin_hoe", Material.STONE_HOE, 0.5f, 2f);
    public static final CustomToolItem TIN_SWORD = weapon("tin_sword", Material.STONE_SWORD, 1);
    public static final CustomToolItem TIN_SPEAR = weapon("tin_spear", Material.STONE_SWORD, 1);
    public static final CustomToolItem TIN_SHEARS = shears("tin_shears", Material.SHEARS);

    private static final NamespacedKey TIN_EQUIP_ASSET = key("tin");

    public static final CustomArmorItem TIN_HELMET = armor("tin_helmet", Material.IRON_HELMET, EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, TIN_EQUIP_ASSET, 1, 0);
    public static final CustomArmorItem TIN_CHESTPLATE = armor("tin_chestplate", Material.IRON_CHESTPLATE, EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, TIN_EQUIP_ASSET, 3, 1);
    public static final CustomArmorItem TIN_LEGGINGS = armor("tin_leggings", Material.IRON_LEGGINGS, EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, TIN_EQUIP_ASSET, 3, 1);
    public static final CustomArmorItem TIN_BOOTS = armor("tin_boots", Material.IRON_BOOTS, EquipmentSlot.FEET, EquipmentSlotGroup.FEET, TIN_EQUIP_ASSET, 1, 0);

    // -----------------------------------------------------------------
    // Copper tools & armor (weak-to-mid tier: no spear texture)
    // -----------------------------------------------------------------

    public static final CustomToolItem COPPER_PICKAXE = miningTool("copper_pickaxe", Material.IRON_PICKAXE, 1.0f, 3f);
    public static final CustomToolItem COPPER_AXE = weapon("copper_axe", Material.IRON_AXE, 2);
    public static final CustomToolItem COPPER_SHOVEL = miningTool("copper_shovel", Material.IRON_SHOVEL, 1.0f, 3f);
    public static final CustomToolItem COPPER_HOE = miningTool("copper_hoe", Material.IRON_HOE, 1.0f, 3f);
    public static final CustomToolItem COPPER_SWORD = weapon("copper_sword", Material.IRON_SWORD, 2);
    public static final CustomToolItem COPPER_SHEARS = shears("copper_shears", Material.SHEARS);

    private static final NamespacedKey COPPER_EQUIP_ASSET = key("copper");

    public static final CustomArmorItem COPPER_HELMET = armor("copper_helmet", Material.IRON_HELMET, EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, COPPER_EQUIP_ASSET, 2, 0);
    public static final CustomArmorItem COPPER_CHESTPLATE = armor("copper_chestplate", Material.IRON_CHESTPLATE, EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, COPPER_EQUIP_ASSET, 4, 1);
    public static final CustomArmorItem COPPER_LEGGINGS = armor("copper_leggings", Material.IRON_LEGGINGS, EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, COPPER_EQUIP_ASSET, 4, 1);
    public static final CustomArmorItem COPPER_BOOTS = armor("copper_boots", Material.IRON_BOOTS, EquipmentSlot.FEET, EquipmentSlotGroup.FEET, COPPER_EQUIP_ASSET, 2, 0);

    // -----------------------------------------------------------------
    // Mythril tools & armor (upper-mid tier, roughly diamond-ish)
    // -----------------------------------------------------------------

    public static final CustomToolItem MYTHRIL_AXE = Items.registerToolItem(
            key("mythril_axe"),
            Material.IRON_AXE,
            itemModel("mythril_axe"),
            List.of(CustomToolItem.AttributeModifierEntry.of(Attribute.BLOCK_BREAK_SPEED, 2.0, AttributeModifier.Operation.ADD_SCALAR)),
            new CustomToolItem.ToolProperties(4f, 1, List.of(
                    new CustomToolItem.ToolProperties.Rule(Material.OBSIDIAN, 8f, true)
            )),
            null,
            (tool, event) -> event.getPlayer().sendMessage("Interaction custom !"),
            null,
            null
    );

    public static final CustomToolItem MYTHRIL_PICKAXE = miningTool("mythril_pickaxe", Material.IRON_PICKAXE, 2.0f, 4f);
    public static final CustomToolItem MYTHRIL_SHOVEL = miningTool("mythril_shovel", Material.IRON_SHOVEL, 2.0f, 4f);
    public static final CustomToolItem MYTHRIL_HOE = miningTool("mythril_hoe", Material.IRON_HOE, 2.0f, 4f);
    public static final CustomToolItem MYTHRIL_SWORD = weapon("mythril_sword", Material.IRON_SWORD, 3);
    public static final CustomToolItem MYTHRIL_SPEAR = weapon("mythril_spear", Material.IRON_SWORD, 3);
    public static final CustomToolItem MYTHRIL_SHEARS = shears("mythril_shears", Material.SHEARS);

    private static final NamespacedKey MYTHRIL_EQUIP_ASSET = key("mythril");

    public static final CustomArmorItem MYTHRIL_HELMET = armor("mythril_helmet", Material.IRON_HELMET, EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, MYTHRIL_EQUIP_ASSET, 2, 1);
    public static final CustomArmorItem MYTHRIL_CHESTPLATE = armor("mythril_chestplate", Material.IRON_CHESTPLATE, EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, MYTHRIL_EQUIP_ASSET, 6, 2);
    public static final CustomArmorItem MYTHRIL_LEGGINGS = armor("mythril_leggings", Material.IRON_LEGGINGS, EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, MYTHRIL_EQUIP_ASSET, 5, 2);
    public static final CustomArmorItem MYTHRIL_BOOTS = armor("mythril_boots", Material.IRON_BOOTS, EquipmentSlot.FEET, EquipmentSlotGroup.FEET, MYTHRIL_EQUIP_ASSET, 2, 1);

    // -----------------------------------------------------------------
    // Adamantium tools & armor (strongest tier: netherite-ish)
    // -----------------------------------------------------------------

    public static final CustomToolItem ADAMANTIUM_PICKAXE = miningTool("adamantium_pickaxe", Material.NETHERITE_PICKAXE, 3.0f, 6f);
    public static final CustomToolItem ADAMANTIUM_AXE = weapon("adamantium_axe", Material.NETHERITE_AXE, 5);
    public static final CustomToolItem ADAMANTIUM_SHOVEL = miningTool("adamantium_shovel", Material.NETHERITE_SHOVEL, 3.0f, 6f);
    public static final CustomToolItem ADAMANTIUM_HOE = miningTool("adamantium_hoe", Material.NETHERITE_HOE, 3.0f, 6f);
    public static final CustomToolItem ADAMANTIUM_SWORD = weapon("adamantium_sword", Material.NETHERITE_SWORD, 5);
    public static final CustomToolItem ADAMANTIUM_SPEAR = weapon("adamantium_spear", Material.NETHERITE_SWORD, 5);
    public static final CustomToolItem ADAMANTIUM_SHEARS = shears("adamantium_shears", Material.SHEARS);

    private static final NamespacedKey ADAMANTIUM_EQUIP_ASSET = key("adamantium");

    public static final CustomArmorItem ADAMANTIUM_HELMET = armor("adamantium_helmet", Material.NETHERITE_HELMET, EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, ADAMANTIUM_EQUIP_ASSET, 4, 2);
    public static final CustomArmorItem ADAMANTIUM_CHESTPLATE = armor("adamantium_chestplate", Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, ADAMANTIUM_EQUIP_ASSET, 8, 3);
    public static final CustomArmorItem ADAMANTIUM_LEGGINGS = armor("adamantium_leggings", Material.NETHERITE_LEGGINGS, EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, ADAMANTIUM_EQUIP_ASSET, 7, 3);
    public static final CustomArmorItem ADAMANTIUM_BOOTS = armor("adamantium_boots", Material.NETHERITE_BOOTS, EquipmentSlot.FEET, EquipmentSlotGroup.FEET, ADAMANTIUM_EQUIP_ASSET, 4, 2);

    // -----------------------------------------------------------------
    // Onyx tools & armor (strongest tier: netherite-ish, gem instead of ingot)
    // -----------------------------------------------------------------

    public static final CustomToolItem ONYX_PICKAXE = miningTool("onyx_pickaxe", Material.NETHERITE_PICKAXE, 3.0f, 6f);
    public static final CustomToolItem ONYX_AXE = weapon("onyx_axe", Material.NETHERITE_AXE, 5);
    public static final CustomToolItem ONYX_SHOVEL = miningTool("onyx_shovel", Material.NETHERITE_SHOVEL, 3.0f, 6f);
    public static final CustomToolItem ONYX_HOE = miningTool("onyx_hoe", Material.NETHERITE_HOE, 3.0f, 6f);
    public static final CustomToolItem ONYX_SWORD = weapon("onyx_sword", Material.NETHERITE_SWORD, 5);
    public static final CustomToolItem ONYX_SPEAR = weapon("onyx_spear", Material.NETHERITE_SWORD, 5);
    public static final CustomToolItem ONYX_SHEARS = shears("onyx_shears", Material.SHEARS);

    private static final NamespacedKey ONYX_EQUIP_ASSET = key("onyx");

    public static final CustomArmorItem ONYX_HELMET = armor("onyx_helmet", Material.NETHERITE_HELMET, EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, ONYX_EQUIP_ASSET, 4, 2);
    public static final CustomArmorItem ONYX_CHESTPLATE = armor("onyx_chestplate", Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, ONYX_EQUIP_ASSET, 8, 3);
    public static final CustomArmorItem ONYX_LEGGINGS = armor("onyx_leggings", Material.NETHERITE_LEGGINGS, EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, ONYX_EQUIP_ASSET, 7, 3);
    public static final CustomArmorItem ONYX_BOOTS = armor("onyx_boots", Material.NETHERITE_BOOTS, EquipmentSlot.FEET, EquipmentSlotGroup.FEET, ONYX_EQUIP_ASSET, 4, 2);

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering items...");

        Items.REGISTRY.freeze();
    }
}
