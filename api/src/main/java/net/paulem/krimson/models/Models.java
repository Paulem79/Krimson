package net.paulem.krimson.models;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.registry.NewFrozenRegistry;
import org.bukkit.NamespacedKey;

import java.util.function.Function;

public class Models {
    public static final NewFrozenRegistry<Model, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    public static<T extends Model> T registerModel(String key, Function<NamespacedKey, T> factory) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);

        T model = factory.apply(identifier);
        REGISTRY.register(model);
        KrimsonPlugin.getInstance().getLogger().info("Registered model: " + identifier.getKey());

        return (T) REGISTRY.getOrThrow(model.getKey());
    }
}
