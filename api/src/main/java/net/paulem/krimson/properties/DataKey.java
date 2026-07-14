package net.paulem.krimson.properties;

import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public record DataKey<T, Z>(NamespacedKey key, PersistentDataType<T, Z> type) {
    public DataKey(String keyName, PersistentDataType<T, Z> type) {
        this(new NamespacedKey(KrimsonPlugin.getInstance(), keyName), type);
    }
}