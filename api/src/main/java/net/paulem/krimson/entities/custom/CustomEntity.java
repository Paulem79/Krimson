package net.paulem.krimson.entities.custom;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.entities.Entities;
import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Base class for custom entities in Krimson.
 * Provides core functionality for spawning, ticking, and managing custom entities.
 */
public class CustomEntity implements RegistryKey<NamespacedKey> {
    private static final String REGISTRY_REFERENCE_ERROR_MESSAGE = "You must clone this registry instance of the custom entity before editing it.";

    @Getter
    protected boolean registryReference; // This is used to check if this instance is a registry instance

    @Getter
    private final NamespacedKey key;

    @Getter
    protected LivingEntity entity; // The actual Minecraft entity

    @Getter
    protected CustomEntityProperties properties;

    /**
     * Create a custom entity with the given key
     */
    public CustomEntity(NamespacedKey key) {
        this.key = key;
        this.registryReference = true; // This is a registry reference
    }

    /**
     * Creates a live instance from this registry template.
     */
    public CustomEntity copyOf() {
        CustomEntity copy = new CustomEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    /**
     * Spawn the custom entity at the given location
     */
    public void spawn(Location location) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (location.getWorld() == null) {
            return;
        }

        // Create the base entity (will be overridden by subclasses)
        this.entity = (LivingEntity) location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ZOMBIE);

        // Set up properties
        this.properties = createProperties(entity);

        // Initialize the entity
        initializeEntity();
    }

    /**
     * Initialize the entity with custom properties
     */
    protected void initializeEntity() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        // Make the entity invisible by default (will be handled by display entities)
        entity.setInvisible(true);
        entity.setSilent(true);
        entity.setAI(false);
        entity.setGravity(false);
        entity.setCollidable(false);

        // Store entity type in PDC
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(new NamespacedKey("krimson", "custom_entity"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        pdc.set(new NamespacedKey("krimson", "entity_type"), org.bukkit.persistence.PersistentDataType.STRING, key.toString());

        // Register with entity tracker
        KrimsonPlugin.getInstance().getLogger().info("Spawning custom entity " + key + " at " + entity.getLocation());
    }

    protected CustomEntityProperties createProperties(LivingEntity entity) {
        return new CustomEntityProperties(entity, this);
    }

    /**
     * Tick method called every game tick
     */
    public void tick() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        // Default tick behavior - can be overridden by subclasses
        if (entity == null || !entity.isValid()) {
            remove();
            return;
        }

        // Update entity position and state
        updateEntityState();
    }

    /**
     * Update the entity state (position, rotation, etc.)
     */
    protected void updateEntityState() {
        // Can be overridden by subclasses for custom behavior
    }

    /**
     * Called when a player interacts with the entity
     */
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.getPlayer().sendMessage("You interacted with custom entity: " + key.toString());
    }

    /**
     * Called when the entity is damaged
     */
    public void onDamage(EntityDamageEvent event) {
        // Default behavior - can be overridden
    }

    /**
     * Called when the entity is damaged by another entity
     */
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Default behavior - can be overridden
    }

    /**
     * Called when the entity dies
     */
    public void onDeath(EntityDeathEvent event) {
        // Default behavior - can be overridden
    }

    /**
     * Remove the custom entity
     */
    public void remove() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (entity != null && entity.isValid()) {
            entity.remove();
        }

        // Clean up any resources
        cleanup();
    }

    /**
     * Clean up any resources associated with the entity
     */
    protected void cleanup() {
        // Can be overridden by subclasses
    }

    /**
     * Get the entity's location
     */
    public Location getLocation() {
        return entity != null ? entity.getLocation() : null;
    }

    /**
     * Set the entity's location
     */
    public void setLocation(Location location) {
        if (entity != null) {
            entity.teleport(location);
        }
    }

    /**
     * Get the entity's velocity
     */
    public Vector getVelocity() {
        return entity != null ? entity.getVelocity() : new Vector();
    }

    /**
     * Set the entity's velocity
     */
    public void setVelocity(Vector velocity) {
        if (entity != null) {
            entity.setVelocity(velocity);
        }
    }

    /**
     * Check if the entity is valid
     */
    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    /**
     * Get the entity's health
     */
    public double getHealth() {
        return entity != null ? entity.getHealth() : 0;
    }

    /**
     * Set the entity's health
     */
    public void setHealth(double health) {
        if (entity != null) {
            entity.setHealth(health);
        }
    }

    /**
     * Get the entity's maximum health
     */
    public double getMaxHealth() {
        return entity != null ? entity.getMaxHealth() : 20.0;
    }

    /**
     * Set the entity's maximum health
     */
    public void setMaxHealth(double maxHealth) {
        if (entity != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            entity.setHealth(Math.min(entity.getHealth(), maxHealth));
        }
    }
}