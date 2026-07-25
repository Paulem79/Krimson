package net.paulem.krimson.models;

import com.google.gson.*;
import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.utils.JsonLoader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

public class BbModel implements Model<ItemDisplay> {

    public static final NamespacedKey INSTANCE_KEY = new NamespacedKey("krimson", "bb_instance_id");
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("krimson", "bb_model_key");
    public static final NamespacedKey PART_KEY = new NamespacedKey("krimson", "bb_part_uuid");

    @Getter
    private final NamespacedKey key;

    @Getter
    private final Map<String, BbElementPart> parts = new LinkedHashMap<>();

    @Getter
    private final Map<String, Map<Integer, Map<String, Matrix4f>>> animations = new HashMap<>();

    private static final Map<String, BukkitTask> activeTasks = new HashMap<>();

    public BbModel(NamespacedKey key) {
        this.key = key;
        JsonObject root = JsonLoader.loadJson("assets/" + key.getNamespace() + "/models/" + key.getKey() + ".bbmodel");
        if (root != null) {
            parseBbModel(root);
        }
    }

    // --- PARSING DU FICHIER BBMODEL ---

    private void parseBbModel(JsonObject root) {
        Map<String, BbGroup> groupsByUuid = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();

        if (root.has("groups")) {
            for (JsonElement elem : root.getAsJsonArray("groups")) {
                parseGroupRecursive(elem.getAsJsonObject(), null, groupsByUuid, parentMap);
            }
        }

        Map<String, JsonObject> rawElements = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement elem : root.getAsJsonArray("elements")) {
                JsonObject cubeObj = elem.getAsJsonObject();
                rawElements.put(cubeObj.get("uuid").getAsString(), cubeObj);
            }
        }

        Map<String, String> elementParentMap = new HashMap<>();
        if (root.has("outliner")) {
            parseOutliner(root.getAsJsonArray("outliner"), null, elementParentMap);
        }

        for (Map.Entry<String, JsonObject> entry : rawElements.entrySet()) {
            String elementUuid = entry.getKey();
            JsonObject cubeObj = entry.getValue();

            if (cubeObj.has("export") && !cubeObj.get("export").getAsBoolean()) {
                continue;
            }

            String parentGroupUuid = elementParentMap.get(elementUuid);
            Matrix4f globalMatrix = computeElementGlobalMatrix(cubeObj, parentGroupUuid, groupsByUuid, parentMap, null);

            // Création de l'ItemStack avec la clé du modèle 1.21.4+
            ItemStack itemStack = new ItemStack(Material.PAPER);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setItemModel(new NamespacedKey(key.getNamespace(), "bb_" + key.getKey() + "_" + elementUuid));
                itemStack.setItemMeta(meta);
            }

            parts.put(elementUuid, new BbElementPart(elementUuid, globalMatrix, itemStack, parentGroupUuid));
        }

        if (root.has("animations")) {
            parseAnimations(root.getAsJsonArray("animations"), groupsByUuid, parentMap, rawElements, elementParentMap);
        }
    }

    private void parseGroupRecursive(JsonObject groupObj, String parentUuid, Map<String, BbGroup> groups, Map<String, String> parentMap) {
        String uuid = groupObj.get("uuid").getAsString();
        String name = groupObj.has("name") ? groupObj.get("name").getAsString() : "group";

        Vector3f origin = parseVector3f(groupObj, "origin", 0f);
        Vector3f rotation = parseVector3f(groupObj, "rotation", 0f);

        groups.put(uuid, new BbGroup(uuid, name, origin, rotation));
        if (parentUuid != null) {
            parentMap.put(uuid, parentUuid);
        }

        if (groupObj.has("children")) {
            for (JsonElement child : groupObj.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    parseGroupRecursive(child.getAsJsonObject(), uuid, groups, parentMap);
                }
            }
        }
    }

    private void parseOutliner(JsonArray outliner, String currentParentUuid, Map<String, String> elementParentMap) {
        for (JsonElement item : outliner) {
            if (item.isJsonPrimitive()) {
                elementParentMap.put(item.getAsString(), currentParentUuid);
            } else if (item.isJsonObject()) {
                JsonObject groupObj = item.getAsJsonObject();
                String groupUuid = groupObj.get("uuid").getAsString();
                if (groupObj.has("children")) {
                    parseOutliner(groupObj.getAsJsonArray("children"), groupUuid, elementParentMap);
                }
            }
        }
    }

    // --- CALCUL DES MATRICES DE TRANSFORMATION ---

    private Matrix4f computeElementGlobalMatrix(
            JsonObject cubeObj,
            String parentGroupUuid,
            Map<String, BbGroup> groups,
            Map<String, String> parentMap,
            Map<String, BbBoneTransform> animTransforms
    ) {
        Vector3f from = parseVector3f(cubeObj, "from", 0f);
        Matrix4f elemMatrix = new Matrix4f();

        // 1. Rotation propre au cube (si origin/rotation définis sur le cube)
        if (cubeObj.has("rotation") || cubeObj.has("origin")) {
            Vector3f origin = parseVector3f(cubeObj, "origin", 0f).div(16.0f);
            Vector3f rot = parseVector3f(cubeObj, "rotation", 0f);

            elemMatrix.translate(origin);
            elemMatrix.rotateXYZ((float) Math.toRadians(rot.x), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.z));
            elemMatrix.translate(new Vector3f(origin).negate());
        }

        // 2. Offset de position du coin "from" + compensation du centrage ItemDisplayTransform.FIXED (+0.5, +0.5, +0.5)
        Vector3f fromMeters = new Vector3f(from).div(16.0f);
        elemMatrix.translate(fromMeters);
        elemMatrix.translate(0.5f, 0.5f, 0.5f);

        // 3. Multiplier par les transformations des groupes parents
        Matrix4f parentChainMatrix = computeParentGroupChain(parentGroupUuid, groups, parentMap, animTransforms);

        return new Matrix4f(parentChainMatrix).mul(elemMatrix);
    }

    private Matrix4f computeParentGroupChain(
            String groupUuid,
            Map<String, BbGroup> groups,
            Map<String, String> parentMap,
            Map<String, BbBoneTransform> animTransforms
    ) {
        Matrix4f result = new Matrix4f();
        if (groupUuid == null) return result;

        List<String> chain = new ArrayList<>();
        String current = groupUuid;
        while (current != null) {
            chain.add(0, current);
            current = parentMap.get(current);
        }

        for (String gUuid : chain) {
            BbGroup group = groups.get(gUuid);
            if (group == null) continue;

            Vector3f pivot = new Vector3f(group.origin()).div(16.0f);
            Vector3f rot = new Vector3f(group.rotation());
            Vector3f posOffset = new Vector3f(0, 0, 0);
            Vector3f scaleOffset = new Vector3f(1, 1, 1);

            if (animTransforms != null && animTransforms.containsKey(gUuid)) {
                BbBoneTransform pose = animTransforms.get(gUuid);
                rot.add(pose.rotation);
                posOffset.add(new Vector3f(pose.position).div(16.0f));
                scaleOffset.mul(pose.scale);
            }

            Matrix4f groupMatrix = new Matrix4f();
            groupMatrix.translate(pivot);
            groupMatrix.translate(posOffset);
            groupMatrix.rotateXYZ((float) Math.toRadians(rot.x), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.z));
            groupMatrix.scale(scaleOffset);
            groupMatrix.translate(new Vector3f(pivot).negate());

            result.mul(groupMatrix);
        }

        return result;
    }

    // --- PARSING DES ANIMATIONS ---

    private void parseAnimations(
            JsonArray animsArray,
            Map<String, BbGroup> groups,
            Map<String, String> parentMap,
            Map<String, JsonObject> rawElements,
            Map<String, String> elementParentMap
    ) {
        for (JsonElement animElem : animsArray) {
            JsonObject animObj = animElem.getAsJsonObject();
            String name = animObj.get("name").getAsString();
            float lengthSec = animObj.has("length") ? animObj.get("length").getAsFloat() : 0f;
            int totalTicks = Math.max(1, Math.round(lengthSec * 20f));

            if (!animObj.has("animators")) continue;

            JsonObject animators = animObj.getAsJsonObject("animators");
            Map<Integer, Map<String, BbBoneTransform>> keyframesByTick = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                String targetUuid = entry.getKey();
                JsonObject animator = entry.getValue().getAsJsonObject();

                if (!animator.has("keyframes")) continue;

                for (JsonElement kfElem : animator.getAsJsonArray("keyframes")) {
                    JsonObject kf = kfElem.getAsJsonObject();
                    float time = kf.get("time").getAsFloat();
                    int tick = Math.round(time * 20f);
                    String channel = kf.get("channel").getAsString();

                    Vector3f dataPoint = parseDataPoint(kf);

                    keyframesByTick
                            .computeIfAbsent(tick, k -> new HashMap<>())
                            .computeIfAbsent(targetUuid, k -> new BbBoneTransform())
                            .applyChannel(channel, dataPoint);
                }
            }

            Map<Integer, Map<String, Matrix4f>> animFrames = new TreeMap<>();

            for (int t = 0; t <= totalTicks; t++) {
                Map<String, BbBoneTransform> currentPose = keyframesByTick.getOrDefault(t, Collections.emptyMap());
                Map<String, Matrix4f> frameMatrices = new HashMap<>();

                for (Map.Entry<String, JsonObject> elemEntry : rawElements.entrySet()) {
                    String elemUuid = elemEntry.getKey();
                    JsonObject cubeObj = elemEntry.getValue();

                    String parentGroupUuid = elementParentMap.get(elemUuid);
                    Matrix4f animatedMatrix = computeElementGlobalMatrix(cubeObj, parentGroupUuid, groups, parentMap, currentPose);

                    frameMatrices.put(elemUuid, animatedMatrix);
                }

                animFrames.put(t, frameMatrices);
            }

            animations.put(name, animFrames);
        }
    }

    // --- SPAWN ET RENDU IN-GAME ---

    public List<ItemDisplay> spawn(Location location) {
        List<ItemDisplay> spawned = new ArrayList<>();
        String instanceId = UUID.randomUUID().toString();

        parts.forEach((uuid, part) -> {
            ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, d -> {
                d.setItemStack(part.itemStack());
                d.setItemDisplayTransform(ItemDisplayTransform.FIXED);
                d.setTransformationMatrix(part.defaultMatrix());
            });

            display.getPersistentDataContainer().set(INSTANCE_KEY, PersistentDataType.STRING, instanceId);
            display.getPersistentDataContainer().set(MODEL_KEY, PersistentDataType.STRING, key.toString());
            display.getPersistentDataContainer().set(PART_KEY, PersistentDataType.STRING, uuid);

            spawned.add(display);
        });

        return spawned;
    }

    public void playAnimation(World world, String instanceId, String animationName) {
        if (!animations.containsKey(animationName)) return;

        Map<Integer, Map<String, Matrix4f>> keyframes = animations.get(animationName);
        if (keyframes.isEmpty()) return;

        Map<String, ItemDisplay> entityMap = new HashMap<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ItemDisplay display) {
                String id = display.getPersistentDataContainer().get(INSTANCE_KEY, PersistentDataType.STRING);
                String partUuid = display.getPersistentDataContainer().get(PART_KEY, PersistentDataType.STRING);

                if (instanceId.equals(id) && partUuid != null) {
                    entityMap.put(partUuid, display);
                }
            }
        }

        if (entityMap.isEmpty()) return;

        int maxTick = Collections.max(keyframes.keySet());
        cancelActiveAnimation(instanceId);

        BukkitTask task = new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                if (currentTick > maxTick) {
                    cancel();
                    activeTasks.remove(instanceId);
                    return;
                }

                Map<String, Matrix4f> frame = keyframes.get(currentTick);
                if (frame != null) {
                    frame.forEach((uuid, matrix) -> {
                        ItemDisplay display = entityMap.get(uuid);
                        if (display != null && display.isValid()) {
                            display.setInterpolationDelay(0);
                            display.setInterpolationDuration(1);
                            display.setTransformationMatrix(matrix);
                        }
                    });
                }

                currentTick++;
            }
        }.runTaskTimer(KrimsonPlugin.getInstance(), 0L, 1L);

        activeTasks.put(instanceId, task);
    }

    public static void cancelActiveAnimation(String instanceId) {
        BukkitTask task = activeTasks.remove(instanceId);
        if (task != null) {
            task.cancel();
        }
    }

    // --- UTILITAIRES DE PARSING ---

    private Vector3f parseVector3f(JsonObject parent, String key, float defaultValue) {
        if (!parent.has(key)) return new Vector3f(defaultValue, defaultValue, defaultValue);
        JsonArray arr = parent.getAsJsonArray(key);
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private Vector3f parseDataPoint(JsonObject kf) {
        if (!kf.has("data_points")) return new Vector3f(0, 0, 0);
        JsonArray dp = kf.getAsJsonArray("data_points");
        JsonObject point = dp.get(0).getAsJsonObject();
        return new Vector3f(
                point.get("x").getAsFloat(),
                point.get("y").getAsFloat(),
                point.get("z").getAsFloat()
        );
    }

    // --- STRUCTURES DE DONNÉES ---

    public record BbGroup(String uuid, String name, Vector3f origin, Vector3f rotation) {}

    public record BbElementPart(String uuid, Matrix4f defaultMatrix, ItemStack itemStack, String parentGroupUuid) {}

    public static class BbBoneTransform {
        @Getter private final Vector3f position = new Vector3f(0, 0, 0);
        @Getter private final Vector3f rotation = new Vector3f(0, 0, 0);
        @Getter private final Vector3f scale = new Vector3f(1, 1, 1);

        public void applyChannel(String channel, Vector3f val) {
            switch (channel) {
                case "position" -> position.set(val);
                case "rotation" -> rotation.set(val);
                case "scale" -> scale.set(val);
            }
        }
    }
}