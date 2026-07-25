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
import java.util.HashMap;
import java.util.Map;

/**
 * Parses the skeleton and animations out of a {@code .bbmodel}.
 *
 * <p>Cube geometry, textures and UVs are deliberately ignored: those were baked into
 * the resource pack by {@code tools/generate_pack.py}. Gson ships inside the server
 * already, so this has no dependencies beyond the Paper API.
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

        Map<String, JsonObject> groups = new HashMap<>();
        if (root.has("groups")) {
            for (JsonElement element : root.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                groups.put(group.get("uuid").getAsString(), group);
            }
        }
        if (root.has("outliner")) {
            for (JsonElement element : root.getAsJsonArray("outliner")) {
                if (element.isJsonObject()) {
                    model.roots.add(buildBone(element.getAsJsonObject(), groups, model));
                }
            }
        }
        parseAnimations(root, model);
        return model;
    }

    private static BbBone buildBone(JsonObject node, Map<String, JsonObject> groups,
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
                    bone.children.add(buildBone(child.getAsJsonObject(), groups, model));
                }
                // Bare-string children are cube uuids; geometry lives in the pack.
            }
        }
        return bone;
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
