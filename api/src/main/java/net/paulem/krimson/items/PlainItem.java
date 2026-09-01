package net.paulem.krimson.items;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.utils.ItemUtils;

import java.util.function.Consumer;

/**
 * A plain custom item with a custom texture (item model) and no special behaviour: raw materials, ingots,
 * gems, and any other item that is neither a block, a tool nor an armor piece.
 */
public class PlainItem extends CustomItem {
    @Getter
    private final Material baseMaterial;
    @Getter
    private final NamespacedKey itemModel;
    @Getter
    @Nullable
    private final Consumer<ItemMeta> extraMeta;

    public PlainItem(NamespacedKey key, Material baseMaterial, NamespacedKey itemModel, @Nullable Consumer<ItemMeta> extraMeta) {
        super(key);

        this.baseMaterial = baseMaterial;
        this.itemModel = itemModel;
        this.extraMeta = extraMeta;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = ItemUtils.getWithRawItemModel(new ItemStack(baseMaterial), itemModel);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(Keys.IDENTIFIER.key(), Keys.IDENTIFIER.type(), getKey().toString());

            if (extraMeta != null) {
                extraMeta.accept(meta);
            }

            stack.setItemMeta(meta);
        }

        return stack;
    }
}
