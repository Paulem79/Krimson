# Add Sound Support to BlockDisplayModel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add sound support to the BlockDisplayModel class to play sounds during animations

**Architecture:** Parse sound definitions from JSON, store them in the model, and play sounds at appropriate animation ticks

**Tech Stack:** Java, Bukkit API, Gson, Minecraft NBT

## Global Constraints
- Must work with existing animation system
- Sounds should be parsed from datapack.sound_keyframes section
- Sound metadata should be parsed from meta.sounds section
- Use Bukkit's sound playing capabilities

---

### Task 1: Add Sound Data Structures

**Files:**
- Modify: `/home/paulem/Documents/Krimson/api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java`

**Interfaces:**
- Produces: SoundFrame record, SoundAnimation record

- [ ] **Step 1: Add sound data structures at the end of the file**

```java
// Add these records before the closing brace of the class
public record SoundFrame(
    int tick,
    String soundCommand
) {}

public record SoundAnimation(
    String name,
    Map<Integer, SoundFrame> soundFrames,
    int durationTicks,
    int stepTicks
) {}
```

- [ ] **Step 2: Add sound storage fields**

```java
// Add these fields after the animations field declaration
@Getter
private final Map<String, SoundAnimation> sounds = new HashMap<>();
```

- [ ] **Step 3: Commit changes**

```bash
cd /home/paulem/Documents/Krimson
git add api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java
git commit -m "feat: add sound data structures"
```

---

### Task 2: Parse Sound Keyframes from JSON

**Files:**
- Modify: `/home/paulem/Documents/Krimson/api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java`

**Interfaces:**
- Consumes: Existing parseJson method
- Produces: parseSoundKeyframes method

- [ ] **Step 1: Add sound keyframe parsing method after parseJson**

```java
private void parseSoundKeyframes(JsonObject datapack) {
    if (!datapack.has("sound_keyframes")) return;
    
    JsonObject soundKeyframes = datapack.getAsJsonObject("sound_keyframes");
    
    // Parse all sound animation children (default, etc.)
    for (Map.Entry<String, JsonElement> soundEntry : soundKeyframes.entrySet()) {
        String animName = soundEntry.getKey();
        JsonObject soundFrames = soundEntry.getValue().getAsJsonObject();
        
        Map<Integer, SoundFrame> frames = new TreeMap<>();
        
        for (Map.Entry<String, JsonElement> frameEntry : soundFrames.entrySet()) {
            int tick = Integer.parseInt(frameEntry.getKey());
            String soundCommand = frameEntry.getValue().getAsString();
            frames.put(tick, new SoundFrame(tick, soundCommand));
        }
        
        // We'll set duration and step ticks later from meta
        sounds.put(animName, new SoundAnimation(animName, frames, 0, 0));
    }
}
```

- [ ] **Step 2: Call parseSoundKeyframes in parseJson method**

```java
// In parseJson method, after parsing anim_keyframes, add:
parseSoundKeyframes(datapack);
```

- [ ] **Step 3: Commit changes**

```bash
cd /home/paulem/Documents/Krimson
git add api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java
git commit -m "feat: add sound keyframe parsing"
```

---

### Task 3: Parse Sound Metadata from JSON

**Files:**
- Modify: `/home/paulem/Documents/Krimson/api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java`

**Interfaces:**
- Consumes: Existing parseJson method
- Produces: parseSoundMetadata method

- [ ] **Step 1: Add sound metadata parsing method after parseSoundKeyframes**

```java
private void parseSoundMetadata(JsonObject root) {
    if (!root.has("meta")) return;
    
    JsonObject meta = root.getAsJsonObject("meta");
    if (!meta.has("sounds")) return;
    
    JsonObject soundsMeta = meta.getAsJsonObject("sounds");
    
    // Parse sound metadata for each sound animation
    for (Map.Entry<String, JsonElement> soundEntry : soundsMeta.entrySet()) {
        String soundName = soundEntry.getKey();
        JsonObject soundData = soundEntry.getValue().getAsJsonObject();
        
        if (sounds.containsKey(soundName)) {
            SoundAnimation existing = sounds.get(soundName);
            int durationTicks = soundData.has("durationTicks") ? soundData.get("durationTicks").getAsInt() : 0;
            int stepTicks = soundData.has("stepTicks") ? soundData.get("stepTicks").getAsInt() : 0;
            
            sounds.put(soundName, new SoundAnimation(
                existing.name(), 
                existing.soundFrames(), 
                durationTicks, 
                stepTicks
            ));
        }
    }
}
```

- [ ] **Step 2: Call parseSoundMetadata in parseJson method**

```java
// In parseJson method, after parsing sound keyframes, add:
parseSoundMetadata(base);
```

- [ ] **Step 3: Commit changes**

```bash
cd /home/paulem/Documents/Krimson
git add api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java
git commit -m "feat: add sound metadata parsing"
```

---

### Task 4: Add Sound Playing Method

**Files:**
- Modify: `/home/paulem/Documents/Krimson/api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java`

**Interfaces:**
- Produces: playSound method

- [ ] **Step 1: Add sound playing method after playAnimationLoop**

```java
private void playSound(World world, Location location, String soundCommand) {
    if (soundCommand == null || soundCommand.isBlank()) return;
    
    // Parse the sound command format: "playsound <sound> <source> <player> <x> <y> <z> <volume> <pitch>"
    String[] parts = soundCommand.split("\\s+");
    if (parts.length < 8) {
        KrimsonPlugin.getInstance().getLogger().warning("[Krimson] Invalid sound command format: " + soundCommand);
        return;
    }
    
    String soundName = parts[1];
    String source = parts[2];
    String playerSelector = parts[3];
    
    try {
        double x = parseCoord(parts[4]);
        double y = parseCoord(parts[5]);
        double z = parseCoord(parts[6]);
        float volume = Float.parseFloat(parts[7]);
        float pitch = parts.length > 8 ? Float.parseFloat(parts[8]) : 1.0f;
        
        // Calculate absolute position based on location
        double absX = location.getX() + x;
        double absY = location.getY() + y;
        double absZ = location.getZ() + z;
        
        // Play sound for all players (simplified - in real implementation you'd parse playerSelector)
        for (org.bukkit.entity.Player player : world.getPlayers()) {
            player.playSound(new org.bukkit.Location(world, absX, absY, absZ), soundName, org.bukkit.SoundCategory.valueOf(source.toUpperCase()), volume, pitch);
        }
    } catch (Exception e) {
        KrimsonPlugin.getInstance().getLogger().warning("[Krimson] Error playing sound: " + e.getMessage());
    }
}
```

- [ ] **Step 2: Commit changes**

```bash
cd /home/paulem/Documents/Krimson
git add api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java
git commit -m "feat: add sound playing method"
```

---

### Task 5: Integrate Sounds into Animation Playback

**Files:**
- Modify: `/home/paulem/Documents/Krimson/api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java`

**Interfaces:**
- Consumes: Existing playAnimation and playAnimationLoop methods
- Produces: Modified animation methods with sound support

- [ ] **Step 1: Modify playAnimation method to include sounds**

```java
// In the BukkitRunnable inside playAnimation, modify the run method:
@Override
public void run() {
    if (currentTick > maxTick) {
        cancel();
        return;
    }

    // Play sounds for this tick
    if (sounds.containsKey(animationName)) {
        SoundAnimation soundAnim = sounds.get(animationName);
        if (soundAnim.soundFrames().containsKey(currentTick)) {
            SoundFrame soundFrame = soundAnim.soundFrames().get(currentTick);
            // Find location from any display entity
            if (!entityMap.isEmpty()) {
                Display firstDisplay = entityMap.values().iterator().next();
                playSound(firstDisplay.getWorld(), firstDisplay.getLocation(), soundFrame.soundCommand());
            }
        }
    }

    List<AnimationFrame> frames = keyframes.get(currentTick);
    if (frames != null) {
        for (AnimationFrame frame : frames) {
            Display display = entityMap.get(frame.partTag());
            if (display != null && display.isValid()) {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(frame.duration());
                display.setTransformationMatrix(frame.transformation());

                if (display instanceof BlockDisplay bd && frame.blockData() != null) {
                    bd.setBlock(frame.blockData());
                }
            }
        }
    }

    currentTick++;
}
```

- [ ] **Step 2: Modify playAnimationLoop method similarly**

```java
// In the BukkitRunnable inside playAnimationLoop, modify the run method:
@Override
public void run() {
    // Play sounds for this tick
    if (sounds.containsKey(animationName)) {
        SoundAnimation soundAnim = sounds.get(animationName);
        if (soundAnim.soundFrames().containsKey(currentTick)) {
            SoundFrame soundFrame = soundAnim.soundFrames().get(currentTick);
            // Find location from any display entity
            if (!entityMap.isEmpty()) {
                Display firstDisplay = entityMap.values().iterator().next();
                playSound(firstDisplay.getWorld(), firstDisplay.getLocation(), soundFrame.soundCommand());
            }
        }
    }

    List<AnimationFrame> frames = keyframes.get(currentTick);
    if (frames != null) {
        for (AnimationFrame frame : frames) {
            Display display = entityMap.get(frame.partTag());
            if (display != null && display.isValid()) {
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(frame.duration());
                display.setTransformationMatrix(frame.transformation());

                if (display instanceof BlockDisplay bd && frame.blockData() != null) {
                    bd.setBlock(frame.blockData());
                }
            }
        }
    }

    currentTick++;
    if (currentTick > maxTick) {
        currentTick = 0;
    }
}
```

- [ ] **Step 3: Commit changes**

```bash
cd /home/paulem/Documents/Krimson
git add api/src/main/java/net/paulem/krimson/models/BlockDisplayModel.java
git commit -m "feat: integrate sounds into animation playback"
```

---

### Task 6: Test the Implementation

**Files:**
- Create: Test JSON file with sounds
- Test: Manual testing in game

- [ ] **Step 1: Create test JSON file**

```bash
mkdir -p /home/paulem/Documents/Krimson/src/main/resources/assets/krimson/models/test_sounds
```

```json
{
  "content": {
    "passengers": [],
    "datapack": {
      "anim_keyframes": {
        "default": {
          "0": [
            "tag=bde_0 type=block_display{block_state:{Name:\"minecraft:stone\"},transformation:[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}"
          ]
        }
      },
      "sound_keyframes": {
        "default": {
          "4": "playsound ui.toast.out block @a ~ ~ ~ 1 1.375",
          "6": "playsound item.book.page_turn block @a ~ ~ ~ 1 1"
        }
      }
    }
  },
  "meta": {
    "sounds": {
      "default": {
        "durationTicks": 12,
        "name": "Default",
        "stepTicks": 2
      }
    }
  }
}
```

- [ ] **Step 2: Test in game**

- Load the model and play the animation
- Verify sounds play at ticks 4 and 6
- Check console for any errors

- [ ] **Step 3: Commit test file**

```bash
cd /home/paulem/Documents/Krimson
git add src/main/resources/assets/krimson/models/test_sounds.json
git commit -m "test: add sound test model"
```

---

## Plan Complete

The implementation plan covers:
1. Adding sound data structures
2. Parsing sound keyframes from JSON
3. Parsing sound metadata from JSON
4. Adding sound playing functionality
5. Integrating sounds into animation playback
6. Testing the implementation

**Plan complete and saved to `/home/paulem/Documents/Krimson/docs/superpowers/plans/2026-07-24-add-sound-support.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**