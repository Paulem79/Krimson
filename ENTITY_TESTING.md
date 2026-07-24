# Entity System Testing Guide

This guide provides instructions for testing the new entity system in Krimson.

## Basic Testing

### 1. Compile the Project

First, ensure the project compiles without errors:

```bash
./gradlew build
```

### 2. Start the Test Server

Start your Minecraft server with the Krimson test plugin installed.

### 3. Test Basic Entity Spawning

Use the `/krimson spawn` command to spawn test entities:

```
/krimson spawn krimsontest:test_mob
```

This should spawn a basic test mob at your location.

### 4. Test Animated Entity

```
/krimson spawn krimsontest:animated_mob
```

This should spawn an animated mob that uses the reading model.

### 5. Check Entity Count

```
/krimson entities
```

This should display the number of custom entities currently loaded.

## Expected Behavior

### Test Mob Entity
- Should appear as an invisible zombie (base entity)
- Should have a custom name "§6Test Mob"
- Should wander around randomly
- Should respond to interactions with a message
- Should play effects when killed

### Animated Mob Entity
- Should display the reading model
- Should have a custom name "§bAnimated Mob"
- Should play animations when moving or interacted with
- Should have higher health (40 HP)
- Should create an explosion effect when killed

## Manual Testing Scenarios

### 1. Entity Interaction
1. Spawn a test mob
2. Right-click the entity
3. **Expected**: Receive a chat message confirming the interaction

### 2. Entity Damage
1. Spawn a test mob
2. Attack the entity
3. **Expected**: Receive a chat message about damaging the entity

### 3. Entity Death
1. Spawn a test mob
2. Kill the entity
3. **Expected**: See explosion effect and receive experience

### 4. Animated Entity Movement
1. Spawn an animated mob
2. Wait for it to move
3. **Expected**: Animation should play when the entity moves

### 5. Multiple Entities
1. Spawn several entities of different types
2. Use `/krimson entities` command
3. **Expected**: Entity count should match the number spawned

## Debugging Commands

### Check Server Logs
```bash
tail -f server.log
```

Look for:
- Entity registration messages
- Entity spawning messages
- Any error messages related to entities

### List All Entities
```
/krimson entities
```

### Check Chunk Data
```
/krimson chunk
```

## Troubleshooting

### Entity doesn't spawn
1. Check server logs for errors
2. Verify the entity key is correct
3. Ensure you're using `/krimson spawn` with the full namespaced key

### Entity disappears immediately
1. Check for errors in the `initializeEntity()` method
2. Verify the entity is being registered with the tracker
3. Look for exceptions in the server console

### Animations don't play
1. Verify the model exists and is registered
2. Check that the animation name is correct
3. Ensure display entities are being spawned

### Commands don't work
1. Verify the Krimson plugin is enabled
2. Check that the command is registered
3. Ensure you have permission to use the command

## Advanced Testing

### Custom Entity Creation

Create your own custom entity:

1. Create a new class extending `CustomEntity` or `ModelEntity`
2. Register it in `PluginEntities.java`
3. Test spawning with `/krimson spawn yourplugin:yourentity`

### Entity AI Testing

Test different AI behaviors:

1. Spawn multiple entities
2. Observe their movement patterns
3. Verify they respond appropriately to players
4. Test edge cases (entities in water, lava, etc.)

### Performance Testing

1. Spawn 50+ entities
2. Monitor server TPS
3. Check for lag or performance issues
4. Profile entity tick methods if needed

## Reporting Issues

If you encounter any issues:

1. Note the exact steps to reproduce
2. Capture relevant server log output
3. Note the expected vs actual behavior
4. Include any error messages

This will help in debugging and improving the entity system.