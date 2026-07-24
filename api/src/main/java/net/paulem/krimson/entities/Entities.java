package net.paulem.krimson.entities;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.entities.custom.CustomEntity;
import net.paulem.krimson.registry.NewFrozenRegistry;
import org.bukkit.NamespacedKey;

import java.util.function.Function;

public class Entities {
    public static final NewFrozenRegistry<CustomEntity, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static<T extends CustomEntity> T register(String key, Function<NamespacedKey, T> factory) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);

        T entity = factory.apply(identifier);
        REGISTRY.register(entity);
        KrimsonPlugin.getInstance().getLogger().info("Registered entity: " + identifier.getKey());

        return (T) REGISTRY.getOrThrow(entity.getKey());
    }
}