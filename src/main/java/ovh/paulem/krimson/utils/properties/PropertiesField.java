package ovh.paulem.krimson.utils.properties;

import lombok.Getter;
import org.bukkit.persistence.PersistentDataType;

public final class PropertiesField<T> {
    @Getter
    private final String fieldName;
    private final T type;

    public PropertiesField(String fieldName, PropertiesStore properties, PersistentDataType<?, T> dataType) {
        this(fieldName, properties.get(fieldName, dataType).orElseThrow());
    }

    public PropertiesField(String fieldName, T type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    public T get() {
        return type;
    }
}
