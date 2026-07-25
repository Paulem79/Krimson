package net.paulem.krimson.models.blockbench.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.paulem.krimson.models.blockbench.BlockbenchDisplayModel;
import net.paulem.krimson.models.blockbench.anim.BbAnimation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class BbModelLoader {
    private BbModelLoader() {
    }

    public static BbModel load(InputStream input, BlockbenchDisplayModel parent) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return parse(root, parent);
        }
    }

    public static BbModel parse(JsonObject root, BlockbenchDisplayModel parent) {
        BbModel model = new BbModel(parent);

        if (root.has("resolution")) {
            JsonObject resolution = root.getAsJsonObject("resolution");
            if (resolution.has("width")) {
                model.textureWidth = resolution.get("width").getAsInt();
            }
            if (resolution.has("height")) {
                model.textureHeight = resolution.get("height").getAsInt();
            }
        }

        Map<String, Integer> textureIndexByUuid = new HashMap<>();
        parseTextures(root, model, textureIndexByUuid);

        Map<String, JsonObject> groups = new HashMap<>();
        if (root.has("groups")) {
            for (JsonElement element : root.getAsJsonArray("groups")) {
                if (element.isJsonObject()) {
                    JsonObject group = element.getAsJsonObject();
                    if (group.has("uuid")) {
                        groups.put(group.get("uuid").getAsString(), group);
                    }
                }
            }
        }

        Map<String, JsonObject> elementsByUuid = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject json = element.getAsJsonObject();
                if (json.has("type") && !"cube".equals(json.get("type").getAsString())) {
                    continue; // meshes non supportés
                }
                if (json.has("uuid")) {
                    elementsByUuid.put(json.get("uuid").getAsString(), json);
                }
            }
        }

        if (root.has("outliner")) {
            for (JsonElement element : root.getAsJsonArray("outliner")) {
                BbBone bone = buildBoneFromNode(element, groups, elementsByUuid, textureIndexByUuid, model);
                if (bone != null) {
                    model.roots.add(bone);
                }
            }
        }
        parseAnimations(root, model);
        return model;
    }

    private static void parseTextures(JsonObject root, BbModel model,
                                      Map<String, Integer> textureIndexByUuid) {
        if (!root.has("textures")) {
            return;
        }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("textures")) {
            JsonObject json = element.getAsJsonObject();
            String name = json.has("name") ? json.get("name").getAsString() : "texture_" + index;
            byte[] pngBytes = null;
            if (json.has("source")) {
                String source = json.get("source").getAsString();
                int comma = source.indexOf(',');
                if (source.startsWith("data:") && comma >= 0) {
                    pngBytes = Base64.getDecoder().decode(source.substring(comma + 1));
                }
            }
            model.textures.add(new BbModel.BbTexture(name, pngBytes));

            if (json.has("uv_width")) {
                model.textureWidth = json.get("uv_width").getAsInt();
            }
            if (json.has("uv_height")) {
                model.textureHeight = json.get("uv_height").getAsInt();
            }

            if (json.has("uuid")) {
                textureIndexByUuid.put(json.get("uuid").getAsString(), index);
            }
            index++;
        }
    }

    private static BbBone buildBoneFromNode(JsonElement element,
                                            Map<String, JsonObject> groups,
                                            Map<String, JsonObject> elementsByUuid,
                                            Map<String, Integer> textureIndexByUuid,
                                            BbModel model) {
        if (element.isJsonPrimitive()) {
            String uuid = element.getAsString();
            JsonObject groupJson = groups.get(uuid);
            if (groupJson != null) {
                return parseBoneObject(groupJson, null, groups, elementsByUuid, textureIndexByUuid, model);
            }
            return null;
        } else if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            String uuid = json.has("uuid") ? json.get("uuid").getAsString() : null;
            JsonObject groupJson = uuid != null ? groups.get(uuid) : null;
            return parseBoneObject(json, groupJson, groups, elementsByUuid, textureIndexByUuid, model);
        }
        return null;
    }

    private static BbBone parseBoneObject(JsonObject node, JsonObject groupOverride,
                                          Map<String, JsonObject> groups,
                                          Map<String, JsonObject> elementsByUuid,
                                          Map<String, Integer> textureIndexByUuid,
                                          BbModel model) {
        String uuid = node.has("uuid") ? node.get("uuid").getAsString()
                : (groupOverride != null && groupOverride.has("uuid") ? groupOverride.get("uuid").getAsString() : "");

        String name = null;
        if (groupOverride != null && groupOverride.has("name")) {
            name = groupOverride.get("name").getAsString();
        } else if (node.has("name")) {
            name = node.get("name").getAsString();
        } else {
            name = uuid;
        }

        BbBone bone = new BbBone(name);

        JsonObject primary = groupOverride != null ? groupOverride : node;
        readVec3(primary, "origin", bone.origin);
        readVec3(primary, "rotation", bone.rotation);

        if (groupOverride != null) {
            if (!groupOverride.has("origin") && node.has("origin")) {
                readVec3(node, "origin", bone.origin);
            }
            if (!groupOverride.has("rotation") && node.has("rotation")) {
                readVec3(node, "rotation", bone.rotation);
            }
        }

        model.bones.put(name, bone);

        JsonArray children = node.has("children") ? node.getAsJsonArray("children")
                : (groupOverride != null && groupOverride.has("children") ? groupOverride.getAsJsonArray("children") : null);

        if (children != null) {
            for (JsonElement child : children) {
                if (child.isJsonObject()) {
                    BbBone childBone = buildBoneFromNode(child, groups, elementsByUuid, textureIndexByUuid, model);
                    if (childBone != null) {
                        bone.children.add(childBone);
                    }
                } else if (child.isJsonPrimitive()) {
                    String childId = child.getAsString();
                    // 1. Est-ce un cube ?
                    JsonObject cubeJson = elementsByUuid.get(childId);
                    if (cubeJson != null) {
                        bone.cubes.add(buildCube(cubeJson, textureIndexByUuid));
                    } else {
                        // 2. Est-ce un sous-groupe référencé par son UUID ?
                        JsonObject childGroupJson = groups.get(childId);
                        if (childGroupJson != null) {
                            BbBone childBone = parseBoneObject(childGroupJson, null, groups, elementsByUuid, textureIndexByUuid, model);
                            if (childBone != null) {
                                bone.children.add(childBone);
                            }
                        }
                    }
                }
            }
        }
        return bone;
    }

    private static BbCube buildCube(JsonObject json, Map<String, Integer> textureIndexByUuid) {
        BbCube cube = new BbCube();
        readVec3(json, "from", cube.from);
        readVec3(json, "to", cube.to);
        readVec3(json, "origin", cube.origin);
        readVec3(json, "rotation", cube.rotation);
        if (json.has("inflate")) {
            cube.inflate = json.get("inflate").getAsFloat();
        }
        if (json.has("visibility")) {
            cube.visible = json.get("visibility").getAsBoolean();
        }
        if (json.has("faces")) {
            JsonObject faces = json.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                JsonObject faceJson = entry.getValue().getAsJsonObject();
                BbCube.Face face = new BbCube.Face();
                if (faceJson.has("uv")) {
                    JsonArray uv = faceJson.getAsJsonArray("uv");
                    if (uv.size() == 4) {
                        face.uvPixels = new float[]{
                                uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                                uv.get(2).getAsFloat(), uv.get(3).getAsFloat()
                        };
                    }
                }
                if (faceJson.has("rotation")) {
                    face.rotation = faceJson.get("rotation").getAsInt();
                }
                face.textureIndex = resolveTextureIndex(faceJson, textureIndexByUuid);
                cube.faces.put(entry.getKey(), face);
            }
        }
        return cube;
    }

    private static int resolveTextureIndex(JsonObject faceJson, Map<String, Integer> textureIndexByUuid) {
        if (!faceJson.has("texture") || faceJson.get("texture").isJsonNull()) {
            return -1;
        }
        JsonElement texture = faceJson.get("texture");
        if (texture.isJsonPrimitive()) {
            var prim = texture.getAsJsonPrimitive();
            if (prim.isNumber()) {
                return prim.getAsInt();
            }
            if (prim.isString()) {
                String str = prim.getAsString();
                Integer index = textureIndexByUuid.get(str);
                if (index != null) {
                    return index;
                }
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }

    private static void parseAnimations(JsonObject root, BbModel model) {
        if (!root.has("animations")) {
            return;
        }
        for (JsonElement element : root.getAsJsonArray("animations")) {
            JsonObject json = element.getAsJsonObject();
            JsonObject animators = json.getAsJsonObject("animators");
            if (animators == null || animators.size() == 0) {
                continue;
            }
            String name = json.get("name").getAsString();
            float length = json.has("length") ? json.get("length").getAsFloat() : 0.0F;
            BbAnimation animation = new BbAnimation(name, length,
                    BbAnimation.LoopMode.parse(
                            json.has("loop") ? json.get("loop").getAsString() : null));

            for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                if (!animator.has("name")
                        || (animator.has("type")
                        && !"bone".equals(animator.get("type").getAsString()))) {
                    continue;
                }
                BbAnimation.BoneTrack track =
                        animation.track(animator.get("name").getAsString());
                if (!animator.has("keyframes")) {
                    continue;
                }
                for (JsonElement kfElement : animator.getAsJsonArray("keyframes")) {
                    JsonObject kf = kfElement.getAsJsonObject();
                    BbAnimation.Channel channel = channelFor(track,
                            kf.get("channel").getAsString());
                    if (channel == null) {
                        continue;
                    }
                    JsonArray points = kf.getAsJsonArray("data_points");
                    if (points == null || points.size() == 0) {
                        continue;
                    }
                    JsonObject point = points.get(0).getAsJsonObject();
                    channel.keyframes.add(new BbAnimation.Keyframe(
                            kf.get("time").getAsFloat(),
                            BbAnimation.Interpolation.parse(kf.has("interpolation")
                                    ? kf.get("interpolation").getAsString() : null),
                            axis(point, "x"), axis(point, "y"), axis(point, "z")));
                }
            }
            animation.finishLoading();
            model.animations.put(name, animation);
        }
    }

    private static BbAnimation.Channel channelFor(BbAnimation.BoneTrack track,
                                                  String channel) {
        return switch (channel) {
            case "position" -> track.position;
            case "rotation" -> track.rotation;
            case "scale" -> track.scale;
            default -> null;
        };
    }

    private static float axis(JsonObject point, String key) {
        JsonElement value = point.get(key);
        if (value == null || value.isJsonNull()) {
            return 0.0F;
        }
        String raw = value.getAsString().trim();
        if (raw.isEmpty()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private static void readVec3(JsonObject json, String key, float[] dst) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return;
        }
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < 3 && i < array.size(); i++) {
            dst[i] = array.get(i).getAsFloat();
        }
    }
}