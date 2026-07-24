package net.paulem.krimson.entities.custom;

import lombok.Getter;
import net.paulem.krimson.properties.PDCWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * Properties for custom entities, stored in the entity's PDC
 */
public class CustomEntityProperties {
    protected final PDCWrapper container;

    @Getter
    private final CustomEntity entity;

    public CustomEntityProperties(LivingEntity entity, CustomEntity customEntity) {
        this.container = new PDCWrapper(entity);
        this.entity = customEntity;
    }

    /**
     * Get the entity's persistent data container
     */
    public PersistentDataContainer getContainer() {
        return container.getContainer();
    }

    /**
     * Save entity properties to PDC
     */
    public void save() {
        // Can be extended by subclasses
    }

    /**
     * Load entity properties from PDC
     */
    public void load() {
        // Can be extended by subclasses
    }
}