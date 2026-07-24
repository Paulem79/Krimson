package net.paulem.krimson.properties;

import com.jeff_media.customblockdata.CustomBlockData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

import java.util.Optional;

@RequiredArgsConstructor
public final class PDCWrapper {
    @Getter
    private final PersistentDataContainer container;

    public PDCWrapper(PersistentDataHolder holder) {
        this(holder.getPersistentDataContainer());
    }

    public PDCWrapper(Block block) {
        this(new CustomBlockData(block, KrimsonPlugin.getInstance()));
    }

    public <T, Z> Optional<Z> get(DataKey<T, Z> key) {
        return Optional.ofNullable(container.get(key.key(), key.type()));
    }

    public <T, Z> Z getOrDefault(DataKey<T, Z> key, Z defaultValue) {
        return container.getOrDefault(key.key(), key.type(), defaultValue);
    }

    public boolean has(DataKey<?, ?> key) {
        return container.has(key.key(), key.type());
    }

    public <T, Z> void set(DataKey<T, Z> key, Z value) {
        container.set(key.key(), key.type(), value);
    }

    public void clear() {
        for (NamespacedKey key : container.getKeys()) {
            container.remove(key);
        }
    }
}