package ovh.paulem.krimson.items;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.constants.Keys;
import ovh.paulem.krimson.function.TriConsumer;
import ovh.paulem.krimson.registry.RegistryKey;
import ovh.paulem.krimson.utils.ItemUtils;

import java.util.function.Consumer;

// TODO : Add item name, description, etc. using a ItemMeta consumer
public class BlockItem implements RegistryKey<NamespacedKey> {
    private final NamespacedKey key;

    @Getter
    private final Material material;
    @Getter
    private final Consumer<ItemMeta> meta;
    @Getter
    private final TriConsumer<BlockItem, @Nullable Player, Location> action;

    public BlockItem(NamespacedKey key, Material material, Consumer<ItemMeta> meta, TriConsumer<BlockItem, @Nullable Player, Location> action) {
        Preconditions.checkState(material.isSolid(), "Material must be solid to be used as a block item.");
        this.key = key;
        this.material = material;
        this.meta = meta;
        this.action = action;
    }

    public ItemStack getItemStack() {
        ItemStack stack = ItemUtils.getWithItemModel(new ItemStack(material), key);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(Krimson.getInstance(), Keys.IDENTIFIER_KEY), PersistentDataType.STRING, key.toString());

            this.meta.accept(meta);

            stack.setItemMeta(meta);
        }

        return stack;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
