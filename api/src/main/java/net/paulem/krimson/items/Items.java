package net.paulem.krimson.items;

import net.paulem.krimson.KrimsonPlugin;
import org.apache.commons.lang3.function.TriConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.registry.NewFrozenRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Items {
    // LinkedHashMap to keep order for /krimson gui
    public static final NewFrozenRegistry<CustomItem, NamespacedKey> REGISTRY = new NewFrozenRegistry<>(LinkedHashMap::new);

    public static CustomBlockItem registerBlockItem(CustomBlock customBlock, TriConsumer<CustomBlock, @Nullable Player, Location> action) {
        NamespacedKey identifier = customBlock.getKey();

        return registerItem(identifier, key -> new CustomBlockItem(identifier, customBlock, action));
    }

    /**
     * Registers a custom tool (pickaxe, axe, sword, hoe, ...). See {@link CustomToolItem} for details.
     */
    public static CustomToolItem registerToolItem(
            NamespacedKey identifier,
            Material baseMaterial,
            NamespacedKey itemModel,
            List<CustomToolItem.AttributeModifierEntry> attributeModifiers,
            @Nullable CustomToolItem.ToolProperties toolProperties,
            @Nullable Consumer<ItemMeta> extraMeta,
            @Nullable BiConsumer<CustomToolItem, PlayerInteractEvent> onInteract,
            @Nullable BiConsumer<CustomToolItem, EntityDamageByEntityEvent> onAttack,
            @Nullable BiConsumer<CustomToolItem, BlockBreakEvent> onBreakBlock
    ) {
        return registerItem(identifier, key -> new CustomToolItem(
                key, baseMaterial, itemModel, attributeModifiers, toolProperties, extraMeta, onInteract, onAttack, onBreakBlock
        ));
    }

    /**
     * Registers a custom armor piece. See {@link CustomArmorItem} for details.
     */
    public static CustomArmorItem registerArmorItem(
            NamespacedKey identifier,
            Material baseMaterial,
            NamespacedKey itemModel,
            EquipmentSlot slot,
            @Nullable NamespacedKey equipAsset,
            @Nullable Sound equipSound,
            List<CustomToolItem.AttributeModifierEntry> attributeModifiers,
            @Nullable Consumer<ItemMeta> extraMeta,
            @Nullable BiConsumer<CustomArmorItem, Player> onEquip,
            @Nullable BiConsumer<CustomArmorItem, Player> onUnequip,
            @Nullable BiConsumer<CustomArmorItem, EntityDamageEvent> onDamaged
    ) {
        return registerItem(identifier, key -> new CustomArmorItem(
                key, baseMaterial, itemModel, slot, equipAsset, equipSound, attributeModifiers, extraMeta, onEquip, onUnequip, onDamaged
        ));
    }

    public static<T extends CustomItem> T registerItem(NamespacedKey identifier, Function<NamespacedKey, T> factory) {
        T item = factory.apply(identifier);
        REGISTRY.register(item);
        KrimsonPlugin.getInstance().getLogger().info("Registered item: " + identifier.getKey());

        return (T) REGISTRY.getOrThrow(item.getKey());
    }
}
