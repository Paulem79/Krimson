package net.paulem.krimson.pdc;

import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DataTypes {
    private DataTypes() {
        /* This utility class should not be instantiated */
    }

    public static final Map<Class<?>, PersistentDataType<?, ?>> REGISTRY = new HashMap<>();

    public static final InventoryDataType INVENTORY_DATA = register(new InventoryDataType());
    public static final ItemStackDataType ITEM_STACK = register(new ItemStackDataType());
    public static final ItemStackArrayDataType ITEM_STACK_ARRAY = register(new ItemStackArrayDataType());

    public static<P, C, T extends PersistentDataType<P, C>> T register(T dataType) {
        REGISTRY.put(dataType.getComplexType(), dataType);
        return dataType;
    }

    public static<P, C> @Nullable PersistentDataType<P, C> corresponds(Class<C> clasz) {
        for (Map.Entry<Class<?>, PersistentDataType<?, ?>> entry : REGISTRY.entrySet()) {
            if (entry.getKey().isAssignableFrom(clasz)) {
                return (PersistentDataType<P, C>) entry.getValue();
            }
        }

        return null;
    }
}
