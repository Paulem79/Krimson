package net.paulem.krimson.models.blockbench.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.paulem.krimson.models.blockbench.anim.BbAnimation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses the skeleton, geometry, textures and animations out of a {@code .bbmodel}.
 *
 * <p>Cube geometry and embedded textures are kept (unlike a skeleton-only parse) so the
 * model can be baked into a resource pack — and generate its own {@code rig.json} — at
 * runtime, with no external tool and no resource pack the client needs beforehand; see
 * {@code BbModelBaker}. Gson ships inside the server already, so this has no dependencies
 * beyond the Paper API.
 *
 * <p>Every keyframe value in this model is a plain number. A model authored with Molang
 * expressions in its keyframes would need an evaluator in {@link #axis}.
 */
public final class BbModelLoader {
    private BbModelLoader() {
    }

    public static BbModel load(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return parse(root);
        }
    }

    public static BbModel parse(JsonObject root) {
        BbModel model = new BbModel();

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
                JsonObject group = element.getAsJsonObject();
                groups.put(group.get("uuid").getAsString(), group);
            }
        }

        Map<String, JsonObject> elementsByUuid = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject json = element.getAsJsonObject();
                if (json.has("type") && !"cube".equals(json.get("type").getAsString())) {
                    continue; // meshes are not supported, only cuboids.
                }
                elementsByUuid.put(json.get("uuid").getAsString(), json);
            }
        }

        if (root.has("outliner")) {
            for (JsonElement element : root.getAsJsonArray("outliner")) {
                if (element.isJsonObject()) {
                    model.roots.add(buildBone(element.getAsJsonObject(), groups,
                            elementsByUuid, textureIndexByUuid, model));
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
            if (json.has("uuid")) {
                textureIndexByUuid.put(json.get("uuid").getAsString(), index);
            }
            index++;
        }
    }

    private static BbBone buildBone(JsonObject node, Map<String, JsonObject> groups,
                                    Map<String, JsonObject> elementsByUuid,
                                    Map<String, Integer> textureIndexByUuid,
                                    BbModel model) {
        String uuid = node.get("uuid").getAsString();
        JsonObject group = groups.get(uuid);
        String name = group != null && group.has("name")
                ? group.get("name").getAsString()
                : uuid;

        BbBone bone = new BbBone(name);
        if (group != null) {
            readVec3(group, "origin", bone.origin);
            readVec3(group, "rotation", bone.rotation);
        }
        model.bones.put(name, bone);

        if (node.has("children")) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    bone.children.add(buildBone(child.getAsJsonObject(), groups,
                            elementsByUuid, textureIndexByUuid, model));
                } else if (child.isJsonPrimitive()) {
                    // Bare-string children are cube uuids, attached to this bone.
                    JsonObject cubeJson = elementsByUuid.get(child.getAsString());
                    if (cubeJson != null) {
                        bone.cubes.add(buildCube(cubeJson, textureIndexByUuid));
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
                face.textureIndex = resolveTextureIndex(faceJson, textureIndexByUuid);
                cube.faces.put(entry.getKey(), face);
            }
        }
        return cube;
    }

    /**
     * A face's {@code texture} field is either a numeric index (older, box_uv exports)
     * or the uuid of an entry in {@code textures[]} (newer, per-face exports).
     */
    private static int resolveTextureIndex(JsonObject faceJson, Map<String, Integer> textureIndexByUuid) {
        if (!faceJson.has("texture") || faceJson.get("texture").isJsonNull()) {
            return -1;
        }
        JsonElement texture = faceJson.get("texture");
        if (texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isNumber()) {
            return texture.getAsInt();
        }
        if (texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isString()) {
            Integer index = textureIndexByUuid.get(texture.getAsString());
            if (index != null) {
                return index;
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

    /** Keyframe axes are strings in .bbmodel, often with trailing newlines. */
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
