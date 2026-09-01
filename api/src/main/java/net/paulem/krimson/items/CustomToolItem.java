package net.paulem.krimson.items;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.utils.ItemUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A custom tool item (pickaxe, axe, sword, hoe, ...) with a custom texture (item model), optional attribute
 * modifiers (see <a href="https://minecraft.wiki/w/Attribute">Attribute</a>) and optional custom interactions.
 *
 * <p>All interaction callbacks are optional: when a callback is {@code null}, only the vanilla behaviour
 * driven by the data components (attribute modifiers, {@code minecraft:tool} rules) applies.</p>
 */
public class CustomToolItem extends CustomItem {
    @Getter
    private final Material baseMaterial;
    @Getter
    private final NamespacedKey itemModel;
    @Getter
    private final List<AttributeModifierEntry> attributeModifiers;
    @Getter
    @Nullable
    private final ToolProperties toolProperties;
    @Getter
    @Nullable
    private final Consumer<ItemMeta> extraMeta;

    /**
     * Called when a player right-clicks (air or block) while holding this tool.
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomToolItem, PlayerInteractEvent> onInteract;

    /**
     * Called when a player hits an entity while holding this tool.
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomToolItem, EntityDamageByEntityEvent> onAttack;

    /**
     * Called when a player breaks a block while holding this tool, before the vanilla break is processed.
     */
    @Getter
    @Nullable
    private final BiConsumer<CustomToolItem, BlockBreakEvent> onBreakBlock;

    public CustomToolItem(
            NamespacedKey key,
            Material baseMaterial,
            NamespacedKey itemModel,
            List<AttributeModifierEntry> attributeModifiers,
            @Nullable ToolProperties toolProperties,
            @Nullable Consumer<ItemMeta> extraMeta,
            @Nullable BiConsumer<CustomToolItem, PlayerInteractEvent> onInteract,
            @Nullable BiConsumer<CustomToolItem, EntityDamageByEntityEvent> onAttack,
            @Nullable BiConsumer<CustomToolItem, BlockBreakEvent> onBreakBlock
    ) {
        super(key);

        this.baseMaterial = baseMaterial;
        this.itemModel = itemModel;
        this.attributeModifiers = List.copyOf(attributeModifiers);
        this.toolProperties = toolProperties;
        this.extraMeta = extraMeta;
        this.onInteract = onInteract;
        this.onAttack = onAttack;
        this.onBreakBlock = onBreakBlock;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = ItemUtils.getWithRawItemModel(new ItemStack(baseMaterial), itemModel);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.IDENTIFIER.key(), Keys.IDENTIFIER.type(), getKey().toString());

            for (AttributeModifierEntry entry : attributeModifiers) {
                meta.addAttributeModifier(entry.attribute(), entry.toBukkitModifier(getKey()));
            }

            if (toolProperties != null) {
                ToolComponent tool = meta.getTool();
                tool.setDefaultMiningSpeed(toolProperties.defaultMiningSpeed());
                tool.setDamagePerBlock(toolProperties.damagePerBlock());
                toolProperties.rules().forEach(rule -> rule.applyTo(tool));
                meta.setTool(tool);
            }

            if (extraMeta != null) {
                extraMeta.accept(meta);
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }

    /**
     * A single attribute modifier to apply to the tool, expressed independently of any particular
     * {@link AttributeModifier} instance so it can be re-created deterministically (stable key) every time
     * {@link #getItemStack()} is called.
     *
     * @param attribute the attribute to modify, see <a href="https://minecraft.wiki/w/Attribute">the wiki</a>
     * @param amount    the modifier amount
     * @param operation how {@code amount} combines with the base value
     * @param slotGroup which equipment slots this modifier is active in (typically {@link EquipmentSlotGroup#HAND})
     */
    public record AttributeModifierEntry(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup) {
        public static AttributeModifierEntry of(Attribute attribute, double amount, AttributeModifier.Operation operation) {
            return new AttributeModifierEntry(attribute, amount, operation, EquipmentSlotGroup.HAND);
        }

        AttributeModifier toBukkitModifier(NamespacedKey itemKey) {
            NamespacedKey modifierKey = new NamespacedKey(itemKey.getNamespace(), itemKey.getKey() + "." + attribute.getKey().getKey());

            return new AttributeModifier(modifierKey, amount, operation, slotGroup);
        }
    }

    /**
     * Mining behaviour driven by the {@code minecraft:tool} data component. See
     * <a href="https://minecraft.wiki/w/Data_component_format#List_of_components">Data component format</a>.
     */
    public record ToolProperties(float defaultMiningSpeed, int damagePerBlock, List<Rule> rules) {
        public record Rule(Material material, @Nullable Float speed, @Nullable Boolean correctForDrops) {
            void applyTo(ToolComponent tool) {
                tool.addRule(material, speed, correctForDrops);
            }
        }
    }
}
