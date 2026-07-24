package net.paulem.krimson.entities.custom;

import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.BlockDisplayModel;
import net.paulem.krimson.models.Models;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom entity that uses a BlockDisplayModel for its visual representation
 */
public class ModelEntity extends CustomEntity {
    @Getter
    private final NamespacedKey modelKey;

    @Getter
    private BlockDisplayModel model;

    @Getter
    private String modelInstanceId;

    @Getter
    private final List<Display> displayEntities = new ArrayList<>();

    /**
     * Create a model entity with the given key and model
     */
    public ModelEntity(NamespacedKey key, NamespacedKey modelKey) {
        super(key);
        this.modelKey = modelKey;
    }

    @Override
    public ModelEntity copyOf() {
        ModelEntity copy = new ModelEntity(this.getKey(), this.modelKey);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        // Load the model
        if (Models.REGISTRY.containsKey(modelKey)) {
            model = Models.REGISTRY.getOrThrow(modelKey);
            spawnModel();
        } else {
            KrimsonPlugin.getInstance().getLogger().warning("Model " + modelKey + " not found for entity " + getKey());
        }
    }

    /**
     * Spawn the model at the entity's location
     */
    protected void spawnModel() {
        if (model != null && entity != null) {
            Location location = entity.getLocation();
            displayEntities.addAll(model.spawn(location));
            modelInstanceId = java.util.UUID.randomUUID().toString();

            // Link display entities to the main entity
            for (Display display : displayEntities) {
                display.setPersistent(false);
                display.setCustomName("model:" + getKey().getKey());
                display.setCustomNameVisible(false);
            }
        }
    }

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Update model position to match entity position
        if (model != null && !displayEntities.isEmpty()) {
            Location entityLocation = entity.getLocation();
            for (Display display : displayEntities) {
                if (display.isValid()) {
                    // Position display entities relative to the main entity
                    display.teleport(entityLocation);
                }
            }
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();

        // Remove model display entities
        if (model != null && modelInstanceId != null) {
            BlockDisplayModel.removeModelInstance(entity.getWorld(), modelInstanceId);
        }
        displayEntities.clear();
    }

    /**
     * Play an animation on this entity's model
     */
    public void playAnimation(String animationName) {
        if (model != null && modelInstanceId != null && entity != null) {
            model.playAnimation(entity.getWorld(), modelInstanceId, animationName);
        }
    }

    /**
     * Play a looping animation on this entity's model
     */
    public void playAnimationLoop(String animationName) {
        if (model != null && modelInstanceId != null && entity != null) {
            model.playAnimationLoop(entity.getWorld(), modelInstanceId, animationName);
        }
    }

    /**
     * Stop the current animation
     */
    public void stopAnimation() {
        if (model != null && modelInstanceId != null) {
            BlockDisplayModel.cancelActiveAnimation(modelInstanceId);
        }
    }

    @Override
    protected ModelEntityProperties createProperties(LivingEntity entity) {
        return new ModelEntityProperties(entity, this);
    }
}