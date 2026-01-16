package net.paulem.krimson.properties;

import com.jeff_media.customblockdata.CustomBlockData;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import net.paulem.krimson.Krimson;
import net.paulem.krimson.utils.PersistentDataUtils;

import java.util.Optional;

public final class PDCWrapper {
    @Getter
    private final CustomBlockData container;

    public PDCWrapper(@NotNull Block block) {
        this(new CustomBlockData(block, Krimson.getInstance()));
    }

    public PDCWrapper(CustomBlockData container) {
        this.container = container;
    }

    public <P, C> Optional<C> get(PropertiesField<C> field) {
        return get(field.getFieldName(), PersistentDataUtils.getCorrespondType(field.get()));
    }

    public <P, C> Optional<C> get(String key, PersistentDataType<P, C> dataType) {
        return Optional.ofNullable(
                getContainer().get(
                        new NamespacedKey(Krimson.getInstance(), key),
                        dataType
                )
        );
    }

    public boolean has(PropertiesField<?> field) {
        return has(field.getFieldName());
    }

    public boolean has(String key) {
        return getContainer().has(
                new NamespacedKey(Krimson.getInstance(), key)
        );
    }

    public <P, C> void set(PropertiesField<C> field) {
        set(field.getFieldName(), field.get());
    }

    public <P, C> void set(String key, C value) {
        getContainer().set(
                new NamespacedKey(Krimson.getInstance(), key),
                PersistentDataUtils.getCorrespondType(value),
                value
        );
    }

    @Override
    public String toString() {
        return "PropertiesStore{" +
                "container=" + container +
                '}';
    }
}
