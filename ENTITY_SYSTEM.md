# Krimson Entity System

The Krimson Entity System allows you to create custom mobs and entities with custom models, animations, hitboxes, and AI behavior. This system is designed to be extendable and easy to use, similar to vanilla Minecraft's entity system.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [Creating Custom Entities](#creating-custom-entities)
  - [Basic Entity](#basic-entity)
  - [Model Entity](#model-entity)
  - [Entity Properties](#entity-properties)
- [Entity Lifecycle](#entity-lifecycle)
- [Entity Events](#entity-events)
- [Entity AI](#entity-ai)
- [Hitboxes and Collision](#hitboxes-and-collision)
- [Animations](#animations)
- [Entity Registration](#entity-registration)
- [Spawning Entities](#spawning-entities)
- [Entity Tracking](#entity-tracking)
- [Complete Examples](#complete-examples)
- [Advanced Topics](#advanced-topics)

## Overview

The Krimson Entity System builds upon the existing block and model systems to provide a comprehensive framework for creating custom entities. Entities can:

- Use custom models from BlockBench or other sources
- Have multiple hitboxes and collision areas
- Support complex animations
- Implement custom AI behaviors
- Be easily extended and customized

## Key Features

| Feature | Description |
|---------|-------------|
| **Custom Models** | Entities can use any BlockDisplayModel for visual representation |
| **Animations** | Support for complex animations with sound effects |
| **Multiple Hitboxes** | Entities can have multiple collision areas |
| **Custom AI** | Extendable AI system for entity behavior |
| **Persistence** | Entity data can be saved and loaded with chunks |
| **Event System** | Comprehensive event handling for entity interactions |
| **Performance** | Optimized ticking and entity management |

## Getting Started

### Requirements

- Krimson API (included with your plugin)
- Spigot/Paper 1.21.4+
- Java 21+

### Minimum Plugin Structure

```
my-plugin/
├── src/main/java/my/plugin/
│   ├── MyPlugin.java           # Main class extending KrimsonPlugin
│   ├── entities/
│   │   ├── PluginEntities.java  # Entity definitions
│   │   └── custom/
│   │       └── MyCustomEntity.java # Custom entity classes
└── src/main/resources/
    ├── plugin.yml
    └── config.yml
```

## Creating Custom Entities

### Basic Entity

The simplest way to create a custom entity is to extend `CustomEntity`:

```java
package my.plugin.entities.custom;

import net.paulem.krimson.entities.custom.CustomEntity;
import net.paulem.krimson.entities.custom.CustomEntityProperties;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public class MyCustomEntity extends CustomEntity {
    public MyCustomEntity(NamespacedKey key) {
        super(key);
    }

    @Override
    public MyCustomEntity copyOf() {
        MyCustomEntity copy = new MyCustomEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        // Customize the base entity
        if (entity != null) {
            entity.setCustomName("§6My Custom Entity");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(50.0);
            entity.setHealth(50.0);
        }
    }

    @Override
    protected CustomEntityProperties createProperties(LivingEntity entity) {
        return new CustomEntityProperties(entity, this);
    }
}
```

### Model Entity

For entities with custom models, extend `ModelEntity`:

```java
package my.plugin.entities.custom;

import net.paulem.krimson.entities.custom.ModelEntity;
import net.paulem.krimson.entities.custom.ModelEntityProperties;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public class MyModelEntity extends ModelEntity {
    public MyModelEntity(NamespacedKey key) {
        super(key, new NamespacedKey("myplugin", "my_model")); // Specify model key
    }

    @Override
    public MyModelEntity copyOf() {
        MyModelEntity copy = new MyModelEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected ModelEntityProperties createProperties(LivingEntity entity) {
        return new ModelEntityProperties(entity, this);
    }
}
```

### Entity Properties

Entity properties are stored in the entity's PersistentDataContainer (PDC). You can extend `CustomEntityProperties` to add custom properties:

```java
public class MyEntityProperties extends CustomEntityProperties {
    public MyEntityProperties(LivingEntity entity, MyCustomEntity customEntity) {
        super(entity, customEntity);
    }

    // Add custom property methods
    public void setCustomProperty(String key, String value) {
        getContainer().set(new NamespacedKey("myplugin", key), PersistentDataType.STRING, value);
    }

    public String getCustomProperty(String key) {
        return getContainer().get(new NamespacedKey("myplugin", key), PersistentDataType.STRING);
    }
}
```

## Entity Lifecycle

Custom entities go through several lifecycle stages:

1. **Registration**: Entity class is registered in the entity registry
2. **Instantiation**: Entity instance is created (registry reference)
3. **Cloning**: `copyOf()` creates a live instance
4. **Spawning**: Entity is spawned in the world
5. **Ticking**: Entity is updated every game tick
6. **Removal**: Entity is removed from the world

### Important: Registry vs Live Instances

- Instances created via `new CustomEntity(key)` are **registry references** (`registryReference = true`)
- Registry references **cannot** be used for spawning, ticking, or interactions
- Always call `copyOf()` to create a **live instance** for spawning

```java
// ✅ Correct
myEntity.copyOf().spawn(location);

// ❌ Incorrect - throws IllegalStateException
myRegistryEntity.spawn(location);
```

## Entity Events

Custom entities can handle various events:

### Interaction Events

```java
@Override
public void onInteract(PlayerInteractAtEntityEvent event) {
    event.getPlayer().sendMessage("You interacted with " + key.toString());
    event.setCancelled(true); // Prevent default interaction
}
```

### Damage Events

```java
@Override
public void onDamage(EntityDamageEvent event) {
    // Handle all damage types
    if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
        onDamageByEntity(damageByEntityEvent);
    }
}

@Override
public void onDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player) {
        Player player = (Player) event.getDamager();
        player.sendMessage("You attacked " + key.toString());
    }
}
```

### Death Events

```java
@Override
public void onDeath(EntityDeathEvent event) {
    event.setDroppedExp(50);
    event.getDrops().add(new ItemStack(Material.DIAMOND));
    entity.getWorld().createExplosion(entity.getLocation(), 3.0f);
}
```

## Entity AI

Entities can implement custom AI behaviors by overriding the `updateEntityState()` method:

### Simple Wandering AI

```java
@Override
protected void updateEntityState() {
    super.updateEntityState();

    if (entity != null && entity.getLocation().getWorld() != null) {
        // Simple wandering behavior
        if (Math.random() < 0.01) { // 1% chance per tick to change direction
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.1;
            entity.setVelocity(entity.getVelocity().add(new Vector(
                Math.cos(angle) * speed,
                0,
                Math.sin(angle) * speed
            )));
        }
    }
}
```

### Target Following AI

```java
@Override
protected void updateEntityState() {
    super.updateEntityState();

    if (entity != null) {
        // Find nearest player
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : entity.getWorld().getPlayers()) {
            double distance = entity.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance && distance < 100) { // Within 10 blocks
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        // Follow the nearest player
        if (nearestPlayer != null && nearestDistance > 4) { // Keep 2 blocks away
            Vector direction = nearestPlayer.getLocation().toVector().subtract(entity.getLocation().toVector());
            direction.normalize().multiply(0.1); // Move at speed 0.1
            entity.setVelocity(direction);
        }
    }
}
```

## Hitboxes and Collision

### Basic Hitbox Setup

```java
@Override
protected void initializeEntity() {
    super.initializeEntity();

    if (entity != null) {
        // Set hitbox size (width, height)
        entity.setSize(1.0f, 2.0f);
        
        // Enable/disable collision
        entity.setCollidable(true);
        
        // Enable/disable gravity
        entity.setGravity(true);
    }
}
```

### Multiple Hitboxes

For complex entities with multiple hitboxes, you can create additional invisible entities:

```java
private List<Entity> hitboxEntities = new ArrayList<>();

@Override
protected void initializeEntity() {
    super.initializeEntity();

    // Create additional hitboxes
    if (entity != null) {
        // Head hitbox
        Entity headHitbox = entity.getWorld().spawnEntity(entity.getLocation().add(0, 1.5, 0), EntityType.ARMOR_STAND);
        headHitbox.setInvisible(true);
        headHitbox.setSilent(true);
        headHitbox.setGravity(false);
        headHitbox.setCollidable(true);
        hitboxEntities.add(headHitbox);

        // Body hitbox
        Entity bodyHitbox = entity.getWorld().spawnEntity(entity.getLocation().add(0, 0.8, 0), EntityType.ARMOR_STAND);
        bodyHitbox.setInvisible(true);
        bodyHitbox.setSilent(true);
        bodyHitbox.setGravity(false);
        bodyHitbox.setCollidable(true);
        hitboxEntities.add(bodyHitbox);
    }
}

@Override
protected void updateEntityState() {
    super.updateEntityState();

    // Update hitbox positions
    if (entity != null && !hitboxEntities.isEmpty()) {
        Location entityLocation = entity.getLocation();
        
        for (int i = 0; i < hitboxEntities.size(); i++) {
            Entity hitbox = hitboxEntities.get(i);
            if (hitbox.isValid()) {
                // Position hitboxes relative to main entity
                hitbox.teleport(entityLocation.add(0, 1.5 - (i * 0.7), 0));
            }
        }
    }
}

@Override
protected void cleanup() {
    super.cleanup();

    // Remove hitbox entities
    hitboxEntities.forEach(Entity::remove);
    hitboxEntities.clear();
}
```

## Animations

Model entities support animations through the BlockDisplayModel system:

### Playing Animations

```java
// Play a specific animation once
entity.playAnimation("walk");

// Play an animation in a loop
entity.playAnimationLoop("idle");

// Stop current animation
entity.stopAnimation();
```

### Animation Events

```java
@Override
public void onInteract(PlayerInteractAtEntityEvent event) {
    // Play animation when interacted with
    playAnimation("interact");
    event.setCancelled(true);
}

@Override
public void onDamage(EntityDamageEvent event) {
    // Play animation when damaged
    playAnimation("hurt");
}

@Override
protected void updateEntityState() {
    super.updateEntityState();

    // Play walk animation when moving
    if (entity.getVelocity().length() > 0.1) {
        if (!isPlayingWalkAnimation) {
            playAnimationLoop("walk");
            isPlayingWalkAnimation = true;
        }
    } else if (isPlayingWalkAnimation) {
        stopAnimation();
        playAnimationLoop("idle");
        isPlayingWalkAnimation = false;
    }
}
```

## Entity Registration

Register your custom entities in your plugin's entity registry:

```java
package my.plugin.entities;

import net.paulem.krimson.entities.Entities;
import my.plugin.entities.custom.MyCustomEntity;
import my.plugin.entities.custom.MyModelEntity;
import org.bukkit.NamespacedKey;

public class PluginEntities {
    public static final MyCustomEntity MY_CUSTOM_ENTITY = Entities.register(
        "my_custom_entity",
        MyCustomEntity::new
    );

    public static final MyModelEntity MY_MODEL_ENTITY = Entities.register(
        "my_model_entity",
        MyModelEntity::new
    );

    public static void init() {
        Entities.REGISTRY.freeze(); // Freeze registry after all entities are registered
    }
}
```

## Spawning Entities

### Programmatic Spawning

```java
// Get entity from registry
CustomEntity entity = PluginEntities.MY_CUSTOM_ENTITY;

// Create a live instance and spawn it
entity.copyOf().spawn(player.getLocation());
```

### Command Spawning

Use the `/krimson spawn` command:

```
/krimson spawn myplugin:my_custom_entity
```

### Event-based Spawning

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    // Spawn entity when player joins
    PluginEntities.MY_CUSTOM_ENTITY.copyOf().spawn(event.getPlayer().getLocation());
}
```

## Entity Tracking

The `CustomEntityTracker` manages all custom entities:

### Accessing the Tracker

```java
CustomEntityTracker tracker = KrimsonAPI.customEntities;
```

### Getting Entities

```java
// Get entity by Bukkit entity
CustomEntity customEntity = tracker.getEntity(bukkitEntity);

// Get all entities
Map<UUID, CustomEntity> allEntities = tracker.getAllEntities();
```

### Checking if Entity is Custom

```java
if (KrimsonAPI.customEntities.isCustomEntity(entity)) {
    // This is a custom entity
}
```

## Complete Examples

### Simple Mob Entity

```java
package my.plugin.entities.custom;

import net.paulem.krimson.entities.custom.CustomEntity;
import net.paulem.krimson.entities.custom.CustomEntityProperties;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class SimpleMobEntity extends CustomEntity {
    public SimpleMobEntity(NamespacedKey key) {
        super(key);
    }

    @Override
    public SimpleMobEntity copyOf() {
        SimpleMobEntity copy = new SimpleMobEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        if (entity != null) {
            entity.setCustomName("§cSimple Mob");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(20.0);
            entity.setHealth(20.0);
        }
    }

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Simple wandering AI
        if (entity != null && Math.random() < 0.02) {
            double angle = Math.random() * Math.PI * 2;
            entity.setVelocity(entity.getVelocity().add(new Vector(
                Math.cos(angle) * 0.05,
                0,
                Math.sin(angle) * 0.05
            )));
        }
    }

    @Override
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.getPlayer().sendMessage("§7You interacted with a Simple Mob!");
        event.setCancelled(true);
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            if (damageByEntityEvent.getDamager() instanceof Player) {
                Player player = (Player) damageByEntityEvent.getDamager();
                player.sendMessage("§7You attacked the Simple Mob!");
            }
        }
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.setDroppedExp(10);
        entity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 10);
    }

    @Override
    protected CustomEntityProperties createProperties(LivingEntity entity) {
        return new CustomEntityProperties(entity, this);
    }
}
```

### Animated Model Entity

```java
package my.plugin.entities.custom;

import net.paulem.krimson.entities.custom.ModelEntity;
import net.paulem.krimson.entities.custom.ModelEntityProperties;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class AnimatedModelEntity extends ModelEntity {
    private boolean isPlayingWalkAnimation = false;

    public AnimatedModelEntity(NamespacedKey key) {
        super(key, new NamespacedKey("myplugin", "my_animated_model"));
    }

    @Override
    public AnimatedModelEntity copyOf() {
        AnimatedModelEntity copy = new AnimatedModelEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        if (entity != null) {
            entity.setCustomName("§bAnimated Model Entity");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(30.0);
            entity.setHealth(30.0);
        }
    }

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Play animations based on movement
        if (entity != null) {
            if (entity.getVelocity().length() > 0.1) {
                if (!isPlayingWalkAnimation) {
                    playAnimationLoop("walk");
                    isPlayingWalkAnimation = true;
                }
            } else if (isPlayingWalkAnimation) {
                stopAnimation();
                playAnimationLoop("idle");
                isPlayingWalkAnimation = false;
            }
        }
    }

    @Override
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.getPlayer().sendMessage("§aYou interacted with an Animated Model Entity!");
        playAnimation("interact");
        event.setCancelled(true);
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        playAnimation("hurt");
    }

    @Override
    protected ModelEntityProperties createProperties(LivingEntity entity) {
        return new ModelEntityProperties(entity, this);
    }
}
```

## Advanced Topics

### Entity Persistence

Entity persistence can be implemented by saving/loading entity data with chunks:

```java
// In your entity class
public void saveToChunk(Chunk chunk) {
    // Save entity data to chunk's PDC
}

public static CustomEntity loadFromChunk(Chunk chunk, NamespacedKey entityKey) {
    // Load entity from chunk data
}
```

### Custom Entity Types

You can create entirely custom entity types by extending the base classes and overriding behavior:

```java
public class BossEntity extends ModelEntity {
    private int phase = 1;
    private int attackCooldown = 0;

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Boss AI logic
        attackCooldown--;
        if (attackCooldown <= 0) {
            performAttack();
            attackCooldown = 60; // 3 seconds cooldown
        }

        // Phase transitions
        if (getHealth() < getMaxHealth() * 0.5 && phase == 1) {
            phase = 2;
            playAnimation("phase_transition");
        }
    }

    private void performAttack() {
        // Implement boss attack logic
    }
}
```

### Entity Networking

For multiplayer compatibility, ensure your entities sync properly:

```java
@Override
protected void initializeEntity() {
    super.initializeEntity();

    if (entity != null) {
        // Make sure entity is visible to all players
        entity.setPersistent(true);
        
        // For display entities
        for (Display display : displayEntities) {
            display.setPersistent(true);
        }
    }
}
```

## API Reference

### Core Classes

| Class | Package | Description |
|-------|---------|-------------|
| `CustomEntity` | `net.paulem.krimson.entities.custom` | Base custom entity class |
| `ModelEntity` | `net.paulem.krimson.entities.custom` | Entity with custom model support |
| `CustomEntityProperties` | `net.paulem.krimson.entities.custom` | Base entity properties |
| `ModelEntityProperties` | `net.paulem.krimson.entities.custom` | Model entity properties |

### Registries

| Class | Package | Description |
|-------|---------|-------------|
| `Entities` | `net.paulem.krimson.entities` | Entity registry and registration |

### Tracking

| Class | Package | Description |
|-------|---------|-------------|
| `CustomEntityTracker` | `net.paulem.krimson.entities` | Tracks and manages custom entities |

### Utility Methods

| Method | Description |
|--------|-------------|
| `KrimsonAPI.customEntities.isCustomEntity(entity)` | Check if entity is custom |
| `KrimsonAPI.customEntities.getEntity(entity)` | Get custom entity wrapper |
| `entity.copyOf()` | Create live instance from registry reference |
| `entity.spawn(location)` | Spawn entity at location |
| `entity.remove()` | Remove entity from world |

## Best Practices

1. **Always use `copyOf()`**: Never spawn registry references directly
2. **Handle null checks**: Always check if `entity != null` before operations
3. **Clean up resources**: Override `cleanup()` to remove display entities and cancel tasks
4. **Optimize ticking**: Keep `updateEntityState()` logic efficient
5. **Use properties**: Store custom data in entity properties for persistence
6. **Handle errors gracefully**: Catch exceptions in tick methods to prevent entity removal
7. **Test thoroughly**: Test entities in various scenarios (chunk loading, world changes, etc.)

## Troubleshooting

### Entity doesn't spawn
- Check that you called `copyOf()` before `spawn()`
- Verify the location is valid and in a loaded world
- Check console for error messages

### Entity disappears after chunk unload
- Ensure entity persistence is implemented
- Check chunk load/unload event handlers

### Animations don't play
- Verify the model has the specified animation
- Check that `modelInstanceId` is set correctly
- Ensure display entities are valid

### Entity causes lag
- Optimize `updateEntityState()` logic
- Reduce complex calculations in tick method
- Limit the number of display entities

## Future Enhancements

The entity system is designed to be extended. Future enhancements may include:

- Advanced pathfinding integration
- Custom entity rendering with shaders
- Entity equipment and inventory systems
- Mountable entities
- Vehicle entities
- Complex entity relationships (pets, minions, etc.)

## License

This entity system is part of the Krimson library and is licensed under the Apache License 2.0.