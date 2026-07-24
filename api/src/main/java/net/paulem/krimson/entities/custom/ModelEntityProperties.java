package net.paulem.krimson.entities.custom;

import net.paulem.krimson.constants.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Properties for model entities
 */
public class ModelEntityProperties extends CustomEntityProperties {
    public ModelEntityProperties(LivingEntity entity, ModelEntity modelEntity) {
        super(entity, modelEntity);
    }

    /**
     * Get the model key for this entity
     */
    public NamespacedKey getModelKey() {
        String modelKeyString = container.get(Keys.MODEL_KEY).orElse(null);
        return modelKeyString != null ? NamespacedKey.fromString(modelKeyString) : null;
    }

    /**
     * Set the model key for this entity
     */
    public void setModelKey(NamespacedKey modelKey) {
        container.set(Keys.MODEL_KEY, modelKey.toString());
    }

    @Override
    public void save() {
        super.save();
        // Save model-specific properties
    }

    @Override
    public void load() {
        super.load();
        // Load model-specific properties
    }
}