package net.paulem.krimson.items;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.utils.ItemUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A custom armor piece with a custom texture (item model + worn equipment asset), optional attribute
 * modifiers (see <a href="https://minecraft.wiki/w/Attribute">Attribute</a>) and optional custom interactions
 * on equip/unequip/hit, driven by the {@code minecraft:equippable} data component (see
 * <a href="https://minecraft.wiki/w/Data_component_format#List_of_components">Data component format</a>).
 *
 * <p>All interaction callbacks are optional: when a callback is {@code null}, only the vanilla behaviour
 * driven by the data components applies.</p>
 */
public class CustomArmorItem extends CustomItem {
    @Getter
    private final Material baseMaterial;
    @Getter
    private final NamespacedKey itemModel;
    @Getter
    private final EquipmentSlot slot;
    @Getter
    @Nullable
    private final NamespacedKey equipAsset;
    @Getter
    @Nullable
    private final Sound equipSound;
    @Getter
    private final List<CustomToolItem.AttributeModifierEntry> attributeModifiers;
    @Getter
    @Nullable
    private final Consumer<ItemMeta> extraMeta;

    /**
     * Called right after a player equips this armor piece (worn slot change detected).
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomArmorItem, Player> onEquip;

    /**
     * Called right after a player unequips this armor piece.
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomArmorItem, Player> onUnequip;

    /**
     * Called when a player wearing this armor piece takes damage, before the vanilla damage is applied.
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomArmorItem, EntityDamageEvent> onDamaged;

    public CustomArmorItem(
            NamespacedKey key,
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
        super(key);

        this.baseMaterial = baseMaterial;
        this.itemModel = itemModel;
        this.slot = slot;
        this.equipAsset = equipAsset;
        this.equipSound = equipSound;
        this.attributeModifiers = List.copyOf(attributeModifiers);
        this.extraMeta = extraMeta;
        this.onEquip = onEquip;
        this.onUnequip = onUnequip;
        this.onDamaged = onDamaged;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = ItemUtils.getWithRawItemModel(new ItemStack(baseMaterial), itemModel);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.IDENTIFIER.key(), Keys.IDENTIFIER.type(), getKey().toString());

            for (CustomToolItem.AttributeModifierEntry entry : attributeModifiers) {
                meta.addAttributeModifier(entry.attribute(), entry.toBukkitModifier(getKey()));
            }

            EquippableComponent equippable = meta.getEquippable();
            equippable.setSlot(slot);
            if (equipAsset != null) {
                equippable.setModel(equipAsset);
            }
            if (equipSound != null) {
                equippable.setEquipSound(equipSound);
            }
            meta.setEquippable(equippable);

            if (extraMeta != null) {
                extraMeta.accept(meta);
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }
}
