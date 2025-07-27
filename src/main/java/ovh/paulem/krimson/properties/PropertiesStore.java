package ovh.paulem.krimson.properties;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import ovh.paulem.krimson.Krimson;
import ovh.paulem.krimson.utils.persistent.PersistentDataUtils;

import java.util.Optional;

public final class PropertiesStore {
    @Getter
    private final PersistentDataContainer container;

    public PropertiesStore(PersistentDataHolder holder) {
        this(holder.getPersistentDataContainer());
    }

    public PropertiesStore(PersistentDataContainer container) {
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
