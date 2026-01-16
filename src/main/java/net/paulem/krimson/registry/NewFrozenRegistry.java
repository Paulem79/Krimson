package net.paulem.krimson.registry;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A registry that can be frozen, preventing any further registrations.
 * This is useful for registries that should not change after a certain point.
 *
 * @param <T> The type of objects stored in the registry, which must implement {@link RegistryKey}.
 * @param <K> The key type used to identify objects in the registry.
 * @author Paulem<br>
 * Based on code by Miles Holder and The-Epic
 */
public class NewFrozenRegistry<T extends RegistryKey<K>, K> extends WriteableRegistry<T, K> {
    private boolean frozen = false;

    public NewFrozenRegistry(final Supplier<Map<K, T>> registrySupplier) {
        super(() -> Map.copyOf(registrySupplier.get()));
    }

    public NewFrozenRegistry() {
        super(HashMap::new);
    }

    public void freeze() {
        if (frozen) {
            throw new IllegalStateException("Registry is already frozen.");
        }

        frozen = true;
    }

    @Override
    public boolean register(@NotNull T object) {
        Preconditions.checkState(!frozen, "Cannot register new objects to the frozen registry " + getClass().getSimpleName());

        return super.register(object);
    }
}
