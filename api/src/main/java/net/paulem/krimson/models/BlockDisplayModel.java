package net.paulem.krimson.models;

import com.google.gson.*;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.registry.RegistryKey;
import net.paulem.krimson.utils.JsonLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockDisplayModel implements RegistryKey<NamespacedKey> {
    public static final NamespacedKey INSTANCE_KEY = new NamespacedKey("krimson", "model_instance_id");
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("krimson", "model_key");
    public static final NamespacedKey PART_KEY = new NamespacedKey("krimson", "model_part_tag");

    @Getter
    private final NamespacedKey key;

    @Getter
    private final Map<String, DisplayPart> parts = new LinkedHashMap<>();

    @Getter
    private final Map<String, Map<Integer, List<AnimationFrame>>> animations = new HashMap<>();

    @Getter
    private final Vector3f originOffset = new Vector3f(0, 0, 0);

    @Getter
    private final boolean animated;

    // Constructeur Legacy (Commande Vanilla /summon)
    public BlockDisplayModel(NamespacedKey key, String command) {
        this.key = key;
        this.animated = false;
        parseCommand(command);
    }

    // Constructeur JSON (Modèle + Animation)
    public BlockDisplayModel(NamespacedKey key) {
        this.key = key;
        this.animated = true;
        JsonObject json = JsonLoader.loadJson("assets/" + key.getNamespace() + "/models/" + key.getKey() + ".json");
        parseJson(json);
    }

    // --- PARSING JSON ---

    private void parseJson(JsonObject root) {
        try {
            // Si le JSON contient un wrapper "content", on descend d'un niveau
            JsonObject base = root.has("content") ? root.getAsJsonObject("content") : root;

            // 1) Initialiser les DisplayParts depuis content.passengers
            parsePassengers(base);

            // 2) Parser les animations
            if (!base.has("datapack")) {
                KrimsonPlugin.getInstance().getLogger().warning("[Krimson] Clé 'datapack' manquante pour le modèle " + key);
                return;
            }

            JsonObject datapack = base.getAsJsonObject("datapack");
            if (!datapack.has("anim_keyframes")) return;

            JsonObject animKeyframes = datapack.getAsJsonObject("anim_keyframes");

            // Parse all animation children (default, open, etc.)
            for (Map.Entry<String, JsonElement> animEntry : animKeyframes.entrySet()) {
                String animName = animEntry.getKey();
                JsonObject animFrames = animEntry.getValue().getAsJsonObject();

                Map<Integer, List<AnimationFrame>> keyframes = new TreeMap<>();

                for (Map.Entry<String, JsonElement> frameEntry : animFrames.entrySet()) {
                    int tick = Integer.parseInt(frameEntry.getKey());
                    JsonArray commands = frameEntry.getValue().getAsJsonArray();

                    List<AnimationFrame> frames = new ArrayList<>();
                    for (JsonElement cmdElem : commands) {
                        String cmd = cmdElem.getAsString();
                        AnimationFrame frame = parseCommandFrame(cmd);
                        if (frame != null) {
                            frames.add(frame);
                        }
                    }
                    keyframes.put(tick, frames);
                }

                animations.put(animName, keyframes);
            }
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().severe("Erreur parsing JSON pour " + key + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- PARSING PASSENGERS ---

    private void parsePassengers(JsonObject base) {
        if (!base.has("passengers")) return;

        JsonArray passengers = base.getAsJsonArray("passengers");
        int index = 0;

        for (JsonElement passengerElem : passengers) {
            String passengerStr = passengerElem.getAsString();
            if (passengerStr == null || passengerStr.isBlank()) continue;

            // Extraire chaque entité { ... } par équilibrage d'accolades
            List<String> entityStrings = extractEntityCompounds(passengerStr);

            for (String entityStr : entityStrings) {
                try {
                    CompoundTag entityTag = TagParser.parseTag(entityStr);

                    // Extraire le tag (ex: "bde_0") depuis le champ Tags
                    String partTag = "bde_" + (index++);
                    if (entityTag.contains("Tags", Tag.TAG_LIST)) {
                        ListTag tags = entityTag.getList("Tags", Tag.TAG_STRING);
                        if (tags.size() > 0) {
                            partTag = tags.getString(0);
                        }
                    }

                    DisplayPart part = parsePart(entityTag);
                    if (part != null) {
                        parts.put(partTag, part);
                    }
                } catch (Exception e) {
                    // Ignorer les entités mal formées
                }
            }
        }
    }

    private List<String> extractEntityCompounds(String text) {
        List<String> entities = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    entities.add(text.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return entities;
    }

    // --- PARSING COMMAND FRAME ---

    private AnimationFrame parseCommandFrame(String cmd) {
        Pattern tagPattern = Pattern.compile("tag=([a-zA-Z0-9_]+)");
        Pattern typePattern = Pattern.compile("type=([a-zA-Z0-9_]+)");

        Matcher tagMatcher = tagPattern.matcher(cmd);
        Matcher typeMatcher = typePattern.matcher(cmd);

        if (!tagMatcher.find()) return null;
        String tag = tagMatcher.group(1);
        String typeStr = typeMatcher.find() ? typeMatcher.group(1) : "block_display";

        int nbtStart = cmd.indexOf('{');
        if (nbtStart == -1) return null;

        try {
            CompoundTag nbt = TagParser.parseTag(cmd.substring(nbtStart));
            Matrix4f matrix = parseTransformation(nbt);
            int duration = nbt.contains("interpolation_duration") ? nbt.getInt("interpolation_duration") : 0;

            BlockData blockData = nbt.contains("block_state") ? parseBlockData(nbt.getCompound("block_state")) : null;
            ItemStack itemStack = nbt.contains("item") ? parseItemStack(nbt.getCompound("item")) : null;
            DisplayType type = typeStr.contains("item") ? DisplayType.ITEM : DisplayType.BLOCK;

            return new AnimationFrame(tag, type, matrix, duration, blockData, itemStack);
        } catch (Exception e) {
            return null;
        }
    }

    // --- PARSING LEGACY COMMAND ---

    private void parseCommand(String command) {
        if (command == null || command.isBlank()) return;
        parseOriginOffset(command);

        int nbtStart = command.indexOf('{');
        if (nbtStart == -1) return;

        try {
            CompoundTag rootTag = TagParser.parseTag(command.substring(nbtStart));
            int index = 0;

            if (isDisplayEntity(rootTag)) {
                DisplayPart part = parsePart(rootTag);
                if (part != null) parts.put("bde_" + (index++), part);
            }

            if (rootTag.contains("Passengers", Tag.TAG_LIST)) {
                ListTag passengers = rootTag.getList("Passengers", Tag.TAG_COMPOUND);
                for (int i = 0; i < passengers.size(); i++) {
                    DisplayPart part = parsePart(passengers.getCompound(i));
                    if (part != null) parts.put("bde_" + (index++), part);
                }
            }
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().severe("Erreur parsing Legacy " + key + " : " + e.getMessage());
        }
    }

    // --- SPAWN & LINKING VIA PDC ---

    public List<Display> spawn(Location location) {
        Location spawnLoc = location.clone().add(originOffset.x(), originOffset.y(), originOffset.z());
        List<Display> spawnedDisplays = new ArrayList<>();
        String instanceId = UUID.randomUUID().toString();

        parts.forEach((tag, part) -> {
            Display display = null;
            if (part.type() == DisplayType.BLOCK) {
                display = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, d -> {
                    d.setBlock(part.blockData() != null ? part.blockData() : Bukkit.createBlockData(Material.STONE));
                    d.setTransformationMatrix(part.transformation());
                });
            } else if (part.type() == DisplayType.ITEM) {
                display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
                    d.setItemStack(part.itemStack() != null ? part.itemStack() : new ItemStack(Material.AIR));
                    d.setItemDisplayTransform(part.itemTransform() != null ? part.itemTransform() : ItemDisplayTransform.NONE);
                    d.setTransformationMatrix(part.transformation());
                });
            }

            if (display != null) {
                // Liaison des entités via PDC
                display.getPersistentDataContainer().set(INSTANCE_KEY, PersistentDataType.STRING, instanceId);
                display.getPersistentDataContainer().set(MODEL_KEY, PersistentDataType.STRING, key.toString());
                display.getPersistentDataContainer().set(PART_KEY, PersistentDataType.STRING, tag);
                spawnedDisplays.add(display);
            }
        });

        return spawnedDisplays;
    }

    // --- ANIMATION ENGINE ---

    public Set<String> getAvailableAnimations() {
        return animations.keySet();
    }

    public void playAnimation(World world, String instanceId, String animationName) {
        if (!animated || animations.isEmpty()) return;

        // Check if the requested animation exists
        if (!animations.containsKey(animationName)) {
            KrimsonPlugin.getInstance().getLogger().warning("[Krimson] Animation '" + animationName + "' not found for model " + key + ". Available animations: " + String.join(", ", animations.keySet()));
            return;
        }

        Map<Integer, List<AnimationFrame>> keyframes = animations.get(animationName);
        if (keyframes.isEmpty()) return;

        Map<String, Display> entityMap = new HashMap<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Display display) {
                String id = display.getPersistentDataContainer().get(INSTANCE_KEY, PersistentDataType.STRING);
                String partTag = display.getPersistentDataContainer().get(PART_KEY, PersistentDataType.STRING);
                if (instanceId.equals(id) && partTag != null) {
                    entityMap.put(partTag, display);
                }
            }
        }

        if (entityMap.isEmpty()) return;

        int maxTick = Collections.max(keyframes.keySet());

        new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (currentTick > maxTick) {
                    cancel();
                    return;
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
        }.runTaskTimer(KrimsonPlugin.getInstance(), 0L, 1L);
    }

    public void playAnimationLoop(World world, String instanceId, String animationName) {
        if (!animated || animations.isEmpty()) return;

        // Check if the requested animation exists
        if (!animations.containsKey(animationName)) {
            KrimsonPlugin.getInstance().getLogger().warning("[Krimson] Animation '" + animationName + "' not found for model " + key + ". Available animations: " + String.join(", ", animations.keySet()));
            return;
        }

        Map<Integer, List<AnimationFrame>> keyframes = animations.get(animationName);
        if (keyframes.isEmpty()) return;

        Map<String, Display> entityMap = new HashMap<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Display display) {
                String id = display.getPersistentDataContainer().get(INSTANCE_KEY, PersistentDataType.STRING);
                String partTag = display.getPersistentDataContainer().get(PART_KEY, PersistentDataType.STRING);
                if (instanceId.equals(id) && partTag != null) {
                    entityMap.put(partTag, display);
                }
            }
        }

        if (entityMap.isEmpty()) return;

        int maxTick = Collections.max(keyframes.keySet());

        new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
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
        }.runTaskTimer(KrimsonPlugin.getInstance(), 0L, 1L);
    }

    // Get the first animation available and play it
    public void playAnimation(World world, String instanceId) {
        if (!animated || animations.isEmpty()) return;

        playAnimation(world, instanceId, animations.keySet().stream().findFirst().orElseThrow());
    }

    public void playAnimationLoop(World world, String instanceId) {
        if (!animated || animations.isEmpty()) return;

        playAnimationLoop(world, instanceId, animations.keySet().stream().findFirst().orElseThrow());
    }

    // --- UTILS PARSING ---

    private void parseOriginOffset(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length >= 5 && parts[0].equalsIgnoreCase("/summon")) {
            try {
                this.originOffset.set(parseCoord(parts[2]), parseCoord(parts[3]), parseCoord(parts[4]));
            } catch (Exception ignored) {}
        }
    }

    private float parseCoord(String coord) {
        return coord.startsWith("~") ? (coord.length() == 1 ? 0f : Float.parseFloat(coord.substring(1))) : Float.parseFloat(coord);
    }

    private boolean isDisplayEntity(CompoundTag tag) {
        return tag.contains("block_state") || tag.contains("item");
    }

    private DisplayPart parsePart(CompoundTag tag) {
        String id = tag.contains("id") ? tag.getString("id") : "";
        Matrix4f matrix = parseTransformation(tag);

        if (id.contains("block_display") || tag.contains("block_state")) {
            return new DisplayPart(DisplayType.BLOCK, matrix, parseBlockData(tag.getCompound("block_state")), null, null);
        }
        if (id.contains("item_display") || tag.contains("item")) {
            ItemStack itemStack = parseItemStack(tag.getCompound("item"));
            ItemDisplayTransform transform = parseItemTransform(tag.getString("item_display"));
            return new DisplayPart(DisplayType.ITEM, matrix, null, itemStack, transform);
        }
        return null;
    }

    private Matrix4f parseTransformation(CompoundTag tag) {
        Matrix4f matrix = new Matrix4f();
        if (tag.contains("transformation", Tag.TAG_LIST)) {
            ListTag list = tag.getList("transformation", Tag.TAG_FLOAT);
            if (list.size() == 16) {
                float[] m = new float[16];
                for (int i = 0; i < 16; i++) m[i] = list.getFloat(i);
                matrix.set(m).transpose();
                return matrix;
            }
        }
        return matrix;
    }

    private BlockData parseBlockData(CompoundTag blockStateTag) {
        if (blockStateTag.isEmpty()) return Bukkit.createBlockData(Material.AIR);
        String name = blockStateTag.getString("Name");
        StringBuilder sb = new StringBuilder(name);
        if (blockStateTag.contains("Properties", Tag.TAG_COMPOUND)) {
            CompoundTag properties = blockStateTag.getCompound("Properties");
            if (!properties.isEmpty()) {
                sb.append("[");
                List<String> propList = new ArrayList<>();
                for (String propKey : properties.getAllKeys()) {
                    propList.add(propKey + "=" + properties.getString(propKey));
                }
                sb.append(String.join(",", propList)).append("]");
            }
        }
        try {
            return Bukkit.createBlockData(sb.toString());
        } catch (IllegalArgumentException e) {
            return Bukkit.createBlockData(Material.STONE);
        }
    }

    private ItemStack parseItemStack(CompoundTag itemTag) {
        if (itemTag.isEmpty()) return new ItemStack(Material.AIR);
        try {
            // Normaliser l'ancien format NBT (Count -> count) pour le codec Data Component 1.21.4+
            if (itemTag.contains("Count") && !itemTag.contains("count")) {
                itemTag.putInt("count", itemTag.getInt("Count"));
                itemTag.remove("Count");
            }

            // Utiliser le codec ItemStack pour parser correctement les components (player_head skins, etc.)
            return net.minecraft.world.item.ItemStack.CODEC
                    .parse(net.minecraft.nbt.NbtOps.INSTANCE, itemTag)
                    .result()
                    .map(net.minecraft.world.item.ItemStack::asBukkitCopy)
                    .orElseGet(() -> fallbackParseItemStack(itemTag));
        } catch (Exception e) {
            return fallbackParseItemStack(itemTag);
        }
    }

    private ItemStack fallbackParseItemStack(CompoundTag itemTag) {
        if (itemTag.isEmpty()) return new ItemStack(Material.AIR);
        Material material = Material.matchMaterial(itemTag.getString("id"));
        int count = itemTag.contains("Count") ? itemTag.getInt("Count") :
                    itemTag.contains("count") ? itemTag.getInt("count") : 1;
        return new ItemStack(material != null ? material : Material.AIR, count);
    }

    private ItemDisplayTransform parseItemTransform(String transformStr) {
        if (transformStr == null || transformStr.isBlank()) return ItemDisplayTransform.NONE;
        try { return ItemDisplayTransform.valueOf(transformStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return ItemDisplayTransform.NONE; }
    }

    // --- STRUCTURES DE DONNÉES ---

    public enum DisplayType { BLOCK, ITEM }

    public record DisplayPart(
            DisplayType type,
            Matrix4f transformation,
            BlockData blockData,
            ItemStack itemStack,
            ItemDisplayTransform itemTransform
    ) {}

    public record AnimationFrame(
            String partTag,
            DisplayType type,
            Matrix4f transformation,
            int duration,
            BlockData blockData,
            ItemStack itemStack
    ) {
        public DisplayPart toPart() {
            return new DisplayPart(type, transformation, blockData, itemStack, ItemDisplayTransform.NONE);
        }
    }
}