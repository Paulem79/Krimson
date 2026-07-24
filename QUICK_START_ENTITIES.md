# Krimson Entities - Quick Start Guide

## Getting Started in 5 Minutes

### 1. Add Entity Initialization to Your Plugin

Update your main plugin class to implement `initEntities()`:

```java
@Override
public void initEntities() {
    PluginEntities.init(); // Your entity registry
}
```

### 2. Create Your First Entity

Create a simple entity class:

```java
package my.plugin.entities.custom;

import net.paulem.krimson.entities.custom.CustomEntity;
import net.paulem.krimson.entities.custom.CustomEntityProperties;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public class MyFirstEntity extends CustomEntity {
    public MyFirstEntity(NamespacedKey key) {
        super(key);
    }

    @Override
    public MyFirstEntity copyOf() {
        MyFirstEntity copy = new MyFirstEntity(this.key);
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();
        if (entity != null) {
            entity.setCustomName("§aMy First Entity");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(50.0);
        }
    }

    @Override
    protected CustomEntityProperties createProperties(LivingEntity entity) {
        return new CustomEntityProperties(entity, this);
    }
}
```

### 3. Register Your Entity

Create an entity registry:

```java
package my.plugin.entities;

import net.paulem.krimson.entities.Entities;
import my.plugin.entities.custom.MyFirstEntity;

public class PluginEntities {
    public static final MyFirstEntity MY_FIRST_ENTITY = Entities.register(
        "my_first_entity",
        MyFirstEntity::new
    );

    public static void init() {
        Entities.REGISTRY.freeze();
    }
}
```

### 4. Spawn Your Entity

Use the command in-game:

```
/krimson spawn myplugin:my_first_entity
```

Or spawn programmatically:

```java
MyFirstEntity entity = PluginEntities.MY_FIRST_ENTITY.copyOf();
entity.spawn(player.getLocation());
```

### 5. Test It Out!

- Right-click your entity → Should see interaction message
- Attack your entity → Should respond to damage
- Kill your entity → Should be removed properly

## Common Entity Types

### Basic Entity
```java
public class SimpleMob extends CustomEntity {
    // Basic entity with no model
}
```

### Model Entity
```java
public class ModelMob extends ModelEntity {
    public ModelMob(NamespacedKey key) {
        super(key, new NamespacedKey("myplugin", "my_model"));
    }
    // Entity with custom model and animations
}
```

### Animated Entity
```java
public class AnimatedMob extends ModelEntity {
    @Override
    protected void updateEntityState() {
        super.updateEntityState();
        if (entity.getVelocity().length() > 0.1) {
            playAnimationLoop("walk");
        } else {
            playAnimationLoop("idle");
        }
    }
}
```

## Key Commands

```
/krimson spawn <entity>      # Spawn an entity
/krimson entities            # Count loaded entities
/krimson give <item>        # Give custom items
/krimson fill <item> <x> <y> <z> # Fill area with blocks
```

## Entity Lifecycle

```
1. Register entity class
2. Create registry reference
3. Call copyOf() for live instance
4. Spawn at location
5. Entity ticks automatically
6. Remove when done
```

## Best Practices

✅ **DO** always call `copyOf()` before spawning
✅ **DO** check `entity != null` before operations
✅ **DO** clean up resources in `cleanup()`
✅ **DO** keep tick logic efficient
✅ **DO** use properties for persistent data

❌ **DON'T** spawn registry references directly
❌ **DON'T** assume entity is always valid
❌ **DON'T** do heavy computations in tick
❌ **DON'T** forget to freeze the registry

## Troubleshooting

**Entity doesn't spawn?**
- Check entity key is correct
- Verify you called `copyOf()`
- Look for errors in console

**Animations not playing?**
- Verify model exists
- Check animation names
- Ensure display entities are valid

**Commands not working?**
- Check plugin is enabled
- Verify command registration
- Ensure proper permissions

## Next Steps

1. **Read the full documentation**: `ENTITY_SYSTEM.md`
2. **Try the test entities**: `krimsontest:test_mob` and `krimsontest:animated_mob`
3. **Experiment with AI**: Override `updateEntityState()`
4. **Add custom models**: Use BlockBench and BlockDisplayModel
5. **Implement persistence**: Save/load entities with chunks

## Example: Simple AI

```java
@Override
protected void updateEntityState() {
    super.updateEntityState();
    
    if (entity != null && Math.random() < 0.02) {
        // Random wandering
        double angle = Math.random() * Math.PI * 2;
        entity.setVelocity(entity.getVelocity().add(new Vector(
            Math.cos(angle) * 0.05,
            0,
            Math.sin(angle) * 0.05
        )));
    }
}
```

## Example: Player Following AI

```java
@Override
protected void updateEntityState() {
    super.updateEntityState();
    
    if (entity != null) {
        Player nearest = findNearestPlayer(10.0);
        if (nearest != null) {
            Vector direction = nearest.getLocation().toVector()
                .subtract(entity.getLocation().toVector())
                .normalize()
                .multiply(0.1);
            entity.setVelocity(direction);
        }
    }
}
```

That's it! You now have a working custom entity system in your Minecraft server. 🎉