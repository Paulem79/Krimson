# Krimson

A library for creating custom blocks in a Spigot/Paper-based Minecraft server environment.

Krimson enables you to create **custom blocks** with unique appearances, behaviors, inventories, and light emission — all without requiring a client-side mod. It leverages `ItemDisplay` entities to render block visuals and `PersistentDataContainer` (PDC) to store block data persistently.

---

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Plugin Setup](#plugin-setup)
- [Creating Custom Items](#creating-custom-items)
- [Creating Custom Blocks](#creating-custom-blocks)
- [Inventory Custom Blocks](#inventory-custom-blocks)
- [Light Blocks](#light-blocks)
- [Registries](#registries)
- [Properties & PDC System](#properties--pdc-system)
- [ItemStack Serialization / Deserialization](#itemstack-serialization--deserialization)
- [Resource Pack Hosting](#resource-pack-hosting)
- [Event System](#event-system)
- [Block Tracking & Ticking](#block-tracking--ticking)
- [Complete Example](#complete-example)
- [Rust JNI Integration](./RUST_JNI_INTEGRATION.md)
- [API Reference](#api-reference)

---

## Overview

| Feature | Description |
|---------|-------------|
| **Custom Blocks** | Blocks with any visual appearance using item models and `ItemDisplay` entities |
| **Inventory Blocks** | Blocks with persistent GUI inventories (chests, barrels, etc.) |
| **Light-emitting Blocks** | Blocks that emit light levels 0–15 |
| **Custom Items** | Items linked to blocks for placement |
| **Resource Packs** | Automatic generation and hosting of per-version resource packs |
| **Persistence** | All block data stored in PDC — survives server restarts |
| **Serialization** | DFU codec-based ItemStack and inventory serialization |
| **Registries** | Freezable registry system for blocks and items |
| **Performance** | Native Rust JNI library for compression and key parsing |
| **Ticking** | Per-block async and sync ticking |
| **Cross-version** | Compatible with Spigot and Paper |

---

## Getting Started

### Requirements

- Spigot / Paper 1.21.4+
- Java 21+
- Gradle or Maven

### Add Krimson as a Dependency

```kotlin
repositories {
    maven("https://repo.paulem.dev/releases")
}

dependencies {
    implementation("net.paulem:krimson-api:VERSION")
}
```

### Minimum Plugin Structure

```
my-plugin/
├── src/main/java/my/plugin/
│   ├── MyPlugin.java           # Main class extending KrimsonPlugin
│   ├── blocks/
│   │   └── PluginBlocks.java   # Block definitions
│   └── items/
│       └── PluginItems.java    # Item definitions
└── src/main/resources/
    ├── plugin.yml
    └── config.yml
```

---

## Plugin Setup

### 1. Extend `KrimsonPlugin<T>`

Your main plugin class must extend `KrimsonPlugin<T>` (where `T` is your own plugin class). Implement:

- `initBlocks()` — called by the API to register blocks
- `initItems()` — called by the API to register items

```java
package my.plugin;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;

public class MyPlugin extends KrimsonPlugin<MyPlugin> {
    private KrimsonAPI<MyPlugin> api;

    @Override
    public void onEnable() {
        super.onEnable(); // REQUIRED: sets up scheduler, config, ViaAPI, native libs

        api = new KrimsonAPI<>(this);
        api.init(true); // true = register the /krimson command
    }

    @Override
    public void onDisable() {
        super.onDisable();
        api.stop(); // Stops the resource pack HTTP server
    }

    @Override
    public void initBlocks() {
        PluginBlocks.init();
    }

    @Override
    public void initItems() {
        PluginItems.init();
    }
}
```

### Static Accessors in `KrimsonPlugin`

| Method | Returns | Description |
|--------|---------|-------------|
| `KrimsonPlugin.getInstance()` | `KrimsonPlugin<?>` | The plugin singleton |
| `KrimsonPlugin.getScheduler()` | `TaskScheduler` | UniversalScheduler (Folia-compatible) |
| `KrimsonPlugin.getConfiguration()` | `FileConfiguration` | Plugin's `config.yml` |
| `KrimsonPlugin.getViaAPI()` | `ViaAPI<Player>` | ViaVersion API for protocol detection |

### What `KrimsonAPI.init()` Does

1. Creates a `CustomBlockTracker` for ticking and tracking custom blocks
2. Calls `plugin.initItems()` then `plugin.initBlocks()`
3. Registers all built-in event listeners
4. Optionally registers the `/krimson` debug command
5. Starts the resource pack HTTP server (Javalin)

---

## Creating Custom Items

### `CustomItem` (Base Class)

Extend `CustomItem` to create custom items. Override `getItemStack()`.

```java
package my.plugin.items;

import net.paulem.krimson.items.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RubyItem extends CustomItem {
    public RubyItem(NamespacedKey key) {
        super(key);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(Material.DIAMOND);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§cRuby");
        meta.setItemModel(new NamespacedKey("minecraft", "red_dye"));
        stack.setItemMeta(meta);
        return stack;
    }
}
```

**Register the item:**

```java
import net.paulem.krimson.items.Items;
import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.NamespacedKey;

public static final RubyItem RUBY = Items.registerItem(
    new NamespacedKey(KrimsonPlugin.getInstance(), "ruby"),
    RubyItem::new
);
```

### `CustomBlockItem` (Block Placement Items)

An item that places a custom block when used. Created via `Items.registerBlockItem()`.

```java
import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.Items;

public static final CustomBlockItem RUBY_BLOCK_ITEM = Items.registerBlockItem(
    PluginBlocks.RUBY_BLOCK,
    (customBlock, player, placeLoc) -> customBlock.copyOf().spawn(placeLoc)
);
```

The action `TriConsumer<CustomBlock, Player, Location>` receives:
- `CustomBlock` — the registry block template
- `Player` — the placing player (nullable)
- `Location` — the placement location

---

## Creating Custom Blocks

### Basic `CustomBlock`

```java
package my.plugin.blocks;

import net.paulem.krimson.blocks.custom.CustomBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;

public class RubyBlock extends CustomBlock {
    public RubyBlock(NamespacedKey key) {
        super(key, key, Material.IRON_BLOCK);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        event.getPlayer().sendMessage("§cYou interacted with a Ruby Block!");
        event.setCancelled(true);
    }
}
```

**Constructor parameters:**

| Parameter | Description |
|-----------|-------------|
| `key` | Unique `NamespacedKey` identifier |
| `dropIdentifier` | `NamespacedKey` of the item to drop when broken (`NamespacedKeyUtils.none()` for no drop) |
| `blockMaterial` | Underlying block material (must satisfy `Material.isBlock()`) |

### Registering Blocks

Use `Blocks.register()`:

```java
import net.paulem.krimson.blocks.Blocks;
import java.util.List;

public static final RubyBlock RUBY_BLOCK = Blocks.register(
    "ruby_block", // plugin key → "myplugin:ruby_block"
    meta -> {     // Consumer<ItemMeta> — customize the display item (nullable)
        meta.setDisplayName("§cRuby Block");
        meta.setLore(List.of("§7A shiny red block"));
    },
    RubyBlock::new // Function<NamespacedKey, T extends CustomBlock>
);
```

### Block Lifecycle Methods

```java
// Called when a player right-clicks the block
@Override
public void onInteract(PlayerInteractEvent event) {
    event.setCancelled(true);
}

// Called when the block is placed by a player
@Override
public void onPlace(BlockPlaceEvent event) { }

// Called when a player breaks the block
@Override
public void onPlayerBreak(BlockBreakEvent event) {
    onBreak(event, event.getPlayer());
}

// Called when the block is broken (any source)
@Override
public void onBreak(Event event, Player player) {
    remove(); // Removes display, clears PDC, drops item
}

// Called when the block's chunk unloads
@Override
public void onUnload() {
    // Default: removes the ItemDisplay entity
}
```

### Tick Methods

```java
// Async tick — runs every tick on a background thread
@Override
public void tickAsync() {
    super.tickAsync(); // Validates block type, updates lighting
}

// Sync tick — runs every tick on the main thread
@Override
public void tickSync() {
    super.tickSync(); // Re-spawns display if missing
}
```

### Item Display Customization

The display item stack can be customized via the `meta` consumer in `Blocks.register()`:

```java
Blocks.register("my_block",
    meta -> {
        meta.setDisplayName("§6Golden Block");
        meta.setItemModel(new NamespacedKey("minecraft", "gold_block"));
        meta.setEnchantmentGlintOverride(true);
    },
    MyBlock::new
);
```

Or override `getItemDisplayStack()` in your subclass for full control.

### Important: Registry vs Live Instances

- Instances created via `new CustomBlock(key, dropMat, blockMat)` are **registry references** (`registryReference = true`)
- Registry references **cannot** be used for placement, removal, or ticking — they'll throw `IllegalStateException`
- Always call `copyOf()` to create a **live instance** for spawning

```java
// ✅ Correct
rubyBlock.copyOf().spawn(location);

// ✅ Correct — placement from item
Items.registerBlockItem(myBlock,
    (customBlock, player, placeLoc) -> customBlock.copyOf().spawn(placeLoc)
);

// ❌ Incorrect — throws IllegalStateException
myRegistryBlock.spawn(location); // reference is still a registry template
```

- Instances loaded from existing world data via `new CustomBlock(block)` are automatically live

---

## Inventory Custom Blocks

Blocks that open a GUI with a persistent inventory (chest-like). Extend `InventoryCustomBlock`.

```java
package my.plugin.blocks;

import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public class SafeBlock extends InventoryCustomBlock {
    public SafeBlock(NamespacedKey key) {
        super(key, key, Material.OAK_WOOD, 3 * 9, "Safe");
    }
}
```

**Constructor parameters:**

| Parameter | Description |
|-----------|-------------|
| `key` | Unique identifier |
| `dropIdentifier` | Item to drop on break |
| `blockInside` | Underlying block material |
| `inventorySize` | Number of slots (e.g. `3*9 = 27`) |
| `inventoryTitle` | GUI title |

### Registering

```java
public static final SafeBlock SAFE = Blocks.register(
    "safe",
    meta -> meta.setDisplayName("§6Safe"),
    SafeBlock::new
);

public static final CustomBlockItem SAFE_ITEM = Items.registerBlockItem(
    PluginBlocks.SAFE,
    (customBlock, player, placeLoc) -> customBlock.copyOf().spawn(placeLoc)
);
```

### Inventory Event Handlers

Override these to customize GUI behavior:

```java
@Override
public void onGuiOpen(InventoryOpenEvent event) { }

@Override
public void onGuiClose(InventoryCloseEvent event) {
    // Inventory is auto-saved to PDC
}

@Override
public void onGuiClick(InventoryClickEvent event) { }

@Override
public void onGuiDrag(InventoryDragEvent event) { }

@Override
public void onGuiMoveItem(InventoryMoveItemEvent event) { }

@Override
public void onGuiPickupItem(InventoryPickupItemEvent event) { }
```

### Inventory Persistence

| Trigger | Action |
|---------|--------|
| GUI closes | Contents encoded to Base64 and saved to PDC |
| Chunk unloads | Inventory saved to PDC |
| Chunk loads | Inventory decoded from PDC |

### `InventoryCustomBlockHolder`

Links the inventory to the block's world position:

```java
InventoryCustomBlock.InventoryCustomBlockHolder holder =
    (InventoryCustomBlock.InventoryCustomBlockHolder) inventory.getHolder();

Location loc = holder.getCustomBlockLoc();
InventoryCustomBlock block = holder.getCustomBlock();
```

---

## Light Blocks

Blocks that emit light by placing a `LIGHT` block above themselves.

```java
package my.plugin.blocks;

import net.paulem.krimson.blocks.custom.LightBlock;
import org.bukkit.NamespacedKey;

public class LampBlock extends LightBlock {
    public LampBlock(NamespacedKey key) {
        super(key, key, 15); // Light level: 0-15
    }
}
```

**Constructor parameters:**

| Parameter | Description |
|-----------|-------------|
| `key` | Unique identifier |
| `dropIdentifier` | Item to drop on break |
| `emittingLightLevel` | Light level (0–15) |

### Behaviors

- Uses `Material.SLIME_BLOCK` as the underlying block (forced)
- On `spawn()`, places a `LIGHT` block above the custom block
- On `tickAsync()`, verifies mask block integrity
- `LightSourcePreventionListener` protects the light block

---

## Registries

Krimson uses `NewFrozenRegistry` — a registry that can be frozen to prevent further modifications.

### Built-in Registries

| Registry | Type | Description |
|----------|------|-------------|
| `Blocks.REGISTRY` | `NewFrozenRegistry<CustomBlock, NamespacedKey>` | All registered custom blocks |
| `Items.REGISTRY` | `NewFrozenRegistry<CustomItem, NamespacedKey>` | All registered custom items |

### Using Registries

```java
// Freeze (after all registrations)
Blocks.REGISTRY.freeze();
Items.REGISTRY.freeze();

// Query
Optional<CustomBlock> block = Blocks.REGISTRY.get(namespacedKey);
CustomBlock block = Blocks.REGISTRY.getOrThrow(namespacedKey);
CustomBlock block = Blocks.REGISTRY.getOrNull(namespacedKey);
Set<NamespacedKey> keys = Items.REGISTRY.keys();
```

### Registry Types

| Class | Mutable | Freezable | Use case |
|-------|---------|-----------|----------|
| `WriteableRegistry<T, K>` | Yes | No | General mutable registry |
| `FrozenRegistry<T, K>` | No | Yes | Immutable from construction |
| `NewFrozenRegistry<T, K>` | Yes | Yes | Mutable until `freeze()` |

---

## Properties & PDC System

Block properties are stored in each block's PDC using `CustomBlockData` (by JEFF-Media-GbR).

### PDCWrapper

Type-safe PDC access:

```java
import net.paulem.krimson.properties.PDCWrapper;
import org.bukkit.persistence.PersistentDataType;

PDCWrapper pdc = new PDCWrapper(block);

// Check existence
if (pdc.has("mykey")) { }

// Read
Optional<String> value = pdc.get("mykey", PersistentDataType.STRING);

// Write
pdc.set("mykey", "myvalue");
```

### PropertiesField

A typed field that stores its value in PDC:

```java
// From existing PDC
PropertiesField<Integer> field = new PropertiesField<>("size", pdc, PersistentDataType.INTEGER);

// With a default value (new block)
PropertiesField<String> field = new PropertiesField<>("title", "Chest");
```

### Properties Hierarchy

```java
Properties (abstract)
└── CustomBlockProperties
    ├── dropIdentifierField   (String)
    ├── blockMaterialField    (String)
    └── displayedItemField    (String)

    └── InventoryCustomBlockProperties
        ├── inventorySizeField    (Integer)
        ├── inventoryTitleField   (String)
        ├── inventoryBase64Field  (byte[])
        └── inventory

    └── LightCustomBlockProperties
        └── emittingLightLevelField (Integer)
```

### PDC Keys (from `Keys`)

| Constant | Key | Type | Description |
|----------|-----|------|-------------|
| `CUSTOM_BLOCK_KEY` | `customblock` | `byte` | Marks block as custom |
| `IDENTIFIER_KEY` | `identifier` | `String` | Block's NamespacedKey |
| `DROP_IDENTIFIER_KEY` | `dropidentifier` | `String` | Drop item key |
| `BLOCK_INSIDE_KEY` | `blockinside` | `String` | Underlying material name |
| `DISPLAYED_ITEM_KEY` | `displayeditem` | `String` | Display ItemStack (Base64 JSON) |
| `INVENTORY_SIZE` | `inventorySize` | `int` | Inventory slot count |
| `INVENTORY_TITLE` | `inventoryTitle` | `String` | GUI title |
| `INVENTORY_DATA` | `inventoryBase64` | `byte[]` | Compressed inventory contents |
| `EMITTING_LIGHT_LEVEL` | `emittingLightLevel` | `int` | Light level 0–15 |

---

## ItemStack Serialization / Deserialization

### `Codecs.ITEM_STACK`

A DFU (DataFixerUpper) `Codec<ItemStack>` for serializing Bukkit ItemStacks to/from Base64-encoded JSON strings.

**Serialization pipeline:**

1. `ItemStack` → platform-specific binary serialization (Paper or Spigot)
2. → ZLib compression (via native Rust or Java fallback)
3. → Base64 encoding
4. → JSON string in PDC

```java
import com.mojang.serialization.JsonOps;
import net.paulem.krimson.codec.Codecs;

// Encode ItemStack → JSON
JsonElement json = Codecs.ITEM_STACK.encodeStart(JsonOps.INSTANCE, itemStack)
    .resultOrPartial(error -> logger.severe(error))
    .orElseThrow();

// Decode JSON → ItemStack
ItemStack item = Codecs.ITEM_STACK.parse(JsonOps.INSTANCE, json)
    .resultOrPartial(error -> logger.severe(error))
    .orElseThrow();
```

### `InventoryData` (Inventory Codec)

An entire inventory is serialized using `InventoryData`:

```java
import net.paulem.krimson.inventories.InventoryData;

// Encode
byte[] compressed = InventoryData.encode(new InventoryData(inventory, "Title"));

// Decode
InventoryData data = InventoryData.decode(compressed);
Inventory restored = data.inventory();
```

### Platform Compatibility

| Server | Serializer Class |
|--------|-----------------|
| **Paper** | `PaperItemSerializer` |
| **Spigot** | `SpigotItemSerializer` |
| **Common** | `ItemSerializerHandler` (abstract) |

The correct handler is selected automatically via `CompatAccess`.

---

## Resource Pack Hosting

Krimson automatically generates and hosts version-specific resource packs.

### How It Works

1. On startup, a lightweight **Javalin HTTP server** is started
2. When a player joins, their **Minecraft protocol version** is detected (ViaVersion)
3. A resource pack is generated for that version using the **Packed** library
4. The pack is hosted at `http://localhost:{port}/krimson-pack-{version}`
5. The player receives the resource pack with `addResourcePack()`

### Resource Pack Generation

Resource packs are created in `ResourcePack.kt` using the `packed` library by RadSteeve. For each registered `CustomBlockItem`, an item model and definition file are added to the pack.

### Hosting Configuration

```yaml
# config.yml
# Resource pack hosting is automatic — no additional config required
```

The HTTP server runs on the same port as the Minecraft server. The pack is forced (players cannot decline).

### `DynamicPackResolver`

Determines the pack format version from the player's Minecraft version:

```java
int packFormat = DynamicPackResolver.getFromVersionName("1.21.4");
```

---

## Event System

Krimson registers several built-in event listeners:

| Listener | Purpose |
|----------|---------|
| `CustomBlockActionListener` | Handles interaction, placement, and inventory GUI events for custom blocks |
| `CustomBlockSuppressionListener` | Suppresses vanilla block behavior (break, physics, explosions, pistons) |
| `BlockItemHandlerListener` | Handles block placement from items and crafts prevention |
| `LightSourcePreventionListener` | Protects light blocks from being broken or overwritten |
| `MigrationListener` | Migrates items in inventories to their current reference (on join & inventory open) |

### CustomBlockActionListener

Handles:
- **Right-click interaction** → routes to `customBlock.onInteract(event)`
- **Block placement** → routes to `customBlock.onPlace(event)`
- **Inventory events** (open, close, click, drag, move items, pickup items) → routes to `InventoryCustomBlock` methods

### BlockItemHandlerListener

Handles:
- **Item placement** — detects when a `CustomBlockItem` is used on a block, consumes the item, and calls the block's placement action
- **Craft prevention** — prevents custom block items from being used in crafting recipes

### MigrationListener

Automatically updates outdated custom block items in inventories to their current reference format:

- On `PlayerJoinEvent` — scans player inventory
- On `InventoryOpenEvent` — scans the opened inventory

---

## Block Tracking & Ticking

### `CustomBlockTracker`

Tracks all custom blocks in the world and manages their lifecycles.

| Method | Description |
|--------|-------------|
| `getBlockAt(Block)` | Returns `CustomBlock` at a location, or null |
| `registerBlock(CustomBlock)` | Adds a block to the tracker |
| `removeBlock(CustomBlock)` | Removes a block from the tracker |
| `handleChunkLoad(Chunk)` | Loads custom blocks from a chunk |
| `handleChunkUnload(Chunk)` | Saves and unloads custom blocks from a chunk |
| `saveChunk(Chunk)` | Manually saves a chunk's custom blocks |

### Container Hierarchy

```
GlobalBlockContainer
└── WorldBlockContainer (per world)
    └── ChunkBlockContainer (per chunk)
        └── SectionContainer (per section)
            ├── ArraySectionContainer
            ├── DynamicSectionContainer
            └── MapContainer
```

### Ticking System

Two tick loops run in parallel:

| Loop | Thread | Interval | Purpose |
|------|--------|----------|---------|
| `tickAsync` | Async | Every 1 tick | Block type validation, light updates |
| `tickSync` | Main | Every 1 tick | Display entity re-spawning |

### Custom Block Detection

`KrimsonAPI.isCustomBlock(Block)` checks if a block has the required PDC keys:

```java
if (KrimsonAPI.isCustomBlock(block)) {
    CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(block);
}
```

### `CustomBlockTypeChecker`

Determines the type of a custom block from its PDC:

```java
CustomBlockTypeChecker checker = new CustomBlockTypeChecker(block);

if (checker.isLightBlock()) {
    LightBlock lightBlock = (LightBlock) checker.get();
} else if (checker.isInventoryBlock()) {
    InventoryCustomBlock invBlock = (InventoryCustomBlock) checker.get();
} else {
    CustomBlock basicBlock = checker.get();
}
```

---

## Complete Example

Here's a full working example of a plugin with a custom block that has an inventory.

### `MyPlugin.java`

```java
package my.plugin;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;

public class MyPlugin extends KrimsonPlugin<MyPlugin> {
    private KrimsonAPI<MyPlugin> api;

    @Override
    public void onEnable() {
        super.onEnable();
        api = new KrimsonAPI<>(this);
        api.init(true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        api.stop();
    }

    @Override
    public void initBlocks() {
        PluginBlocks.init();
    }

    @Override
    public void initItems() {
        PluginItems.init();
    }
}
```

### `blocks/PluginBlocks.java`

```java
package my.plugin.blocks;

import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.KrimsonPlugin;

public class PluginBlocks {

    public static final InventoryCustomBlock SAFE = Blocks.register(
        "safe",
        meta -> meta.setDisplayName("§6Safe"),
        key -> new InventoryCustomBlock(key, key, org.bukkit.Material.IRON_BLOCK, 3 * 9, "Safe")
    );

    public static void init() {
        Blocks.REGISTRY.freeze();
    }
}
```

### `items/PluginItems.java`

```java
package my.plugin.items;

import net.paulem.krimson.items.CustomBlockItem;
import net.paulem.krimson.items.Items;
import my.plugin.blocks.PluginBlocks;

public class PluginItems {

    public static final CustomBlockItem SAFE_ITEM = Items.registerBlockItem(
        PluginBlocks.SAFE,
        (customBlock, player, placeLoc) -> customBlock.copyOf().spawn(placeLoc)
    );

    public static void init() {
        Items.REGISTRY.freeze();
    }
}
```

### `plugin.yml`

```yaml
name: MyPlugin
version: 1.0.0
main: my.plugin.MyPlugin
api-version: "1.21"
commands:
  krimson:
    description: Krimson debug command
    usage: /krimson
```

---

## Rust JNI Integration

For performance-critical operations, Krimson includes a native Rust library:

- **Key Parsing** — fast string slicing instead of regex for block key parsing
- **Compression/Decompression** — ZLib via `flate2` crate (2–5× faster than Java)

See [RUST_JNI_INTEGRATION.md](./RUST_JNI_INTEGRATION.md) for detailed documentation.

---

## API Reference

### Core Classes

| Class | Package | Description |
|-------|---------|-------------|
| `KrimsonPlugin<T>` | `net.paulem.krimson.common` | Base plugin class with static accessors |
| `KrimsonAPI<T>` | `net.paulem.krimson` | Main API entry point |
| `CustomBlock` | `net.paulem.krimson.blocks.custom` | Base custom block class |
| `InventoryCustomBlock` | `net.paulem.krimson.blocks.custom` | Block with GUI inventory |
| `LightBlock` | `net.paulem.krimson.blocks.custom` | Block that emits light |
| `CustomItem` | `net.paulem.krimson.items` | Base custom item class |
| `CustomBlockItem` | `net.paulem.krimson.items` | Item that places a custom block |

### Registries

| Class | Package | Description |
|-------|---------|-------------|
| `Registry<T, K>` | `net.paulem.krimson.registry` | Registry interface |
| `WriteableRegistry<T, K>` | `net.paulem.krimson.registry` | Mutable registry |
| `FrozenRegistry<T, K>` | `net.paulem.krimson.registry` | Immutable registry |
| `NewFrozenRegistry<T, K>` | `net.paulem.krimson.registry` | Freezable registry |
| `Blocks` | `net.paulem.krimson.blocks` | Block registry + registration utility |
| `Items` | `net.paulem.krimson.items` | Item registry + registration utility |

### Properties & PDC

| Class | Package | Description |
|-------|---------|-------------|
| `PDCWrapper` | `net.paulem.krimson.properties` | Type-safe PDC wrapper |
| `Properties` | `net.paulem.krimson.properties` | Abstract base for block properties |
| `PropertiesField<T>` | `net.paulem.krimson.properties` | Typed PDC field |
| `CustomBlockProperties` | `net.paulem.krimson.blocks.custom` | Properties for `CustomBlock` |
| `InventoryCustomBlockProperties` | `net.paulem.krimson.blocks.custom` | Properties for `InventoryCustomBlock` |
| `LightCustomBlockProperties` | `net.paulem.krimson.blocks.custom` | Properties for `LightBlock` |
| `Keys` | `net.paulem.krimson.constants` | PDC key constants |

### Serialization

| Class | Package | Description |
|-------|---------|-------------|
| `Codecs` | `net.paulem.krimson.codec` | Codec registry |
| `ItemStackDFUCodec` | `net.paulem.krimson.codec.dfu` | DFU codec for ItemStack |
| `InventoryData` | `net.paulem.krimson.inventories` | Inventory serialization/deserialization |
| `InventoryDiff` | `net.paulem.krimson.inventories` | Detects inventory changes |

### Tracking & Regions

| Class | Package | Description |
|-------|---------|-------------|
| `CustomBlockTracker` | `net.paulem.krimson.regions` | Manages all custom blocks |
| `BlockHolder` | `net.paulem.krimson.regions` | Holds a block reference |
| `ChunkKey` | `net.paulem.krimson.regions` | Chunk coordinate key |
| `CustomBlockTypeChecker` | `net.paulem.krimson.blocks.custom` | Determines block type from PDC |

### Resource Pack

| Class | Package | Description |
|-------|---------|-------------|
| `ResourcePackHosting` | `net.paulem.krimson.resourcepack` | HTTP server and pack distribution |
| `DynamicPackResolver` | `net.paulem.krimson.resourcepack` | Maps versions to pack formats |
| `ResourcePackKt` | `net.paulem.krimson.resourcepack.creator` | Pack generation (Kotlin) |

### Utilities

| Class | Package | Description |
|-------|---------|-------------|
| `BlockUtils` | `net.paulem.krimson.utils` | Block lighting and placement helpers |
| `ChunkUtils` | `net.paulem.krimson.utils` | Chunk iteration utilities |
| `CustomBlockUtils` | `net.paulem.krimson.utils` | Custom block suppression helpers |
| `ItemUtils` | `net.paulem.krimson.utils` | Item model helpers |
| `NamespacedKeyUtils` | `net.paulem.krimson.utils` | Key comparison utilities |
| `PersistentDataUtils` | `net.paulem.krimson.utils` | PDC scanning utilities |
| `NativeUtil` | `net.paulem.krimson.utils` | Rust JNI bridge |
| `ZLibUtils` | `net.paulem.krimson.utils` | Compression/decompression |

### Listeners

| Class | Package | Description |
|-------|---------|-------------|
| `CustomBlockActionListener` | `net.paulem.krimson.listeners` | Interaction & GUI events |
| `CustomBlockSuppressionListener` | `net.paulem.krimson.listeners` | Physics suppression |
| `BlockItemHandlerListener` | `net.paulem.krimson.listeners` | Item placement & crafting |
| `LightSourcePreventionListener` | `net.paulem.krimson.listeners` | Light block protection |
| `MigrationListener` | `net.paulem.krimson.listeners` | Item migration |
| `InventoryListener` | `net.paulem.krimson.listeners` | General inventory events |

### Codec System (net.paulem.krimson.common.versioned)

| Class | Description |
|-------|-------------|
| `ItemSerializerHandler` | Abstract ItemStack serializer |
| `InputStreamHandler` | Abstract input stream reader |
| `OutputStreamHandler` | Abstract output stream writer |

---

## License

Apache License 2.0 — see [LICENSE](./LICENSE).