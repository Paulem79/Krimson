package net.paulem.krimson.models.blockbench.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.paulem.krimson.models.blockbench.rig.RigPart;
import net.paulem.krimson.resourcepack.creator.ParentModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bakes a {@link BbModel}'s geometry and textures into vanilla item models, the same way
 * {@code tools/generate_pack.py} used to do offline. Doing this at runtime, when the
 * model is loaded, is what lets the resource pack be generated and served to players
 * without anyone needing to build or install one by hand beforehand.
 *
 * <h2>Parts</h2>
 * A cube can't be individually animated by the client — only a whole {@code ItemDisplay}
 * can be moved — so cubes are grouped into rigid "parts" that each become one item model
 * and one display entity. Every cube that shares a bone <b>and</b> a cube-level
 * rotation/pivot moves together, so it can share a part; this mirrors exactly how
 * {@code generate_pack.py} grouped cubes, so models baked this way behave identically to
 * ones baked by the old offline tool.
 *
 * <h2>Coordinates</h2>
 * A part's cubes are recentred so the geometry's bounding-box centre lands on model
 * coordinate (8,8,8) — where an {@code ItemDisplay}'s origin renders. {@link RigPart}
 * then carries that original centre (in the model's absolute Blockbench-unit space) so
 * {@code ModelInstance} can place the display correctly at runtime; the cube-level
 * rotation/pivot are carried the same way rather than being baked into the vanilla
 * element json, since vanilla only allows one axis at a snapped angle, but the plugin can
 * apply the true rotation itself via the display's transformation matrix.
 */
public final class BbModelBaker {
    private static final String[] FACE_NAMES = {"north", "east", "south", "west", "up", "down"};
    private static final float ROUND_EPSILON = 1.0E-4F;

    private BbModelBaker() {
    }

    /** One rigid group of cubes, ready to become one item model and one display entity. */
    public record BakedPart(String id, String bone, JsonObject itemModelJson,
                            float[] center, float[] rotation, float[] pivot,
                            boolean visibleByDefault) {
    }

    /** Everything {@code ResourcePack.kt} needs to fold this model into the pack. */
    public record BakeResult(List<BakedPart> parts, Map<String, byte[]> textures,
                             List<String> warnings) {
    }

    public static BakeResult bake(BbModel model, String modelBaseName) {
        List<String> textureKeys = new ArrayList<>();
        Map<String, byte[]> textures = new LinkedHashMap<>();
        for (int i = 0; i < model.textures.size(); i++) {
            String key = modelBaseName + "_tex" + i;
            textureKeys.add(key);
            byte[] png = model.textures.get(i).pngBytes;
            if (png != null) {
                textures.put(key, png);
            }
        }

        Map<String, PartAccumulator> bySignature = new LinkedHashMap<>();
        for (BbBone root : model.roots) {
            collectParts(root, bySignature);
        }

        List<BakedPart> parts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int ordinal = 0;
        for (PartAccumulator accumulator : bySignature.values()) {
            BakedPart part = bakePart(accumulator, modelBaseName, ordinal, textureKeys, model, warnings);
            if (part != null) {
                parts.add(part);
                ordinal++;
            }
        }
        return new BakeResult(parts, textures, warnings);
    }

    // ------------------------------------------------------------------ grouping

    private static final class PartAccumulator {
        final String bone;
        final float[] rotation;
        final float[] pivot;
        final List<BbCube> cubes = new ArrayList<>();

        PartAccumulator(String bone, float[] rotation, float[] pivot) {
            this.bone = bone;
            this.rotation = rotation;
            this.pivot = pivot;
        }
    }

    private static void collectParts(BbBone bone, Map<String, PartAccumulator> bySignature) {
        for (BbCube cube : bone.cubes) {
            boolean rotated = hasRotation(cube.rotation);
            float[] rotation = rotated ? cube.rotation : new float[3];
            float[] pivot = rotated ? cube.origin : new float[3];
            String signature = bone.name + "|" + key(rotation) + "|" + key(pivot);

            PartAccumulator accumulator = bySignature.computeIfAbsent(signature,
                    ignored -> new PartAccumulator(bone.name, rotation, pivot));
            accumulator.cubes.add(cube);
        }
        for (BbBone child : bone.children) {
            collectParts(child, bySignature);
        }
    }

    private static boolean hasRotation(float[] rotation) {
        return Math.abs(rotation[0]) > ROUND_EPSILON || Math.abs(rotation[1]) > ROUND_EPSILON
                || Math.abs(rotation[2]) > ROUND_EPSILON;
    }

    private static String key(float[] v) {
        return Math.round(v[0] * 1000.0F) + "," + Math.round(v[1] * 1000.0F) + ","
                + Math.round(v[2] * 1000.0F);
    }

    // ------------------------------------------------------------------ per-part bake

    private static BakedPart bakePart(PartAccumulator accumulator, String modelBaseName, int ordinal,
                                      List<String> textureKeys, BbModel model, List<String> warnings) {
        float[] minimum = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] maximum = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (BbCube cube : accumulator.cubes) {
            float[] low = new float[3];
            float[] high = new float[3];
            bounds(cube, low, high);
            for (int axis = 0; axis < 3; axis++) {
                minimum[axis] = Math.min(minimum[axis], low[axis]);
                maximum[axis] = Math.max(maximum[axis], high[axis]);
            }
        }
        float[] center = {
                (minimum[0] + maximum[0]) / 2.0F,
                (minimum[1] + maximum[1]) / 2.0F,
                (minimum[2] + maximum[2]) / 2.0F,
        };

        JsonArray elements = new JsonArray();
        for (BbCube cube : accumulator.cubes) {
            JsonObject elementJson = cubeToJson(cube, center, textureKeys.size(), model, warnings);
            if (elementJson != null) {
                elements.add(elementJson);
            }
        }
        if (elements.isEmpty()) {
            return null; // every face was hidden/untextured: nothing to render.
        }

        String id = modelBaseName + "_p" + String.format("%02d", ordinal);

        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", ParentModel.GENERATED.getParent());
        JsonObject texturesJson = new JsonObject();
        for (int i = 0; i < textureKeys.size(); i++) {
            texturesJson.addProperty("tex" + i, "krimson:block/" + textureKeys.get(i));
        }
        if (!textureKeys.isEmpty()) {
            texturesJson.addProperty("particle", "krimson:block/" + textureKeys.get(0));
        }
        itemModel.add("textures", texturesJson);
        itemModel.add("elements", elements);

        boolean visibleByDefault = accumulator.cubes.stream().anyMatch(cube -> cube.visible);

        return new BakedPart(id, accumulator.bone, itemModel, center, accumulator.rotation,
                accumulator.pivot, visibleByDefault);
    }

    private static void bounds(BbCube cube, float[] outLow, float[] outHigh) {
        for (int axis = 0; axis < 3; axis++) {
            float min = Math.min(cube.from[axis], cube.to[axis]);
            float max = Math.max(cube.from[axis], cube.to[axis]);
            outLow[axis] = min - cube.inflate;
            outHigh[axis] = max + cube.inflate;
        }
    }

    private static JsonObject cubeToJson(BbCube cube, float[] center, int textureCount,
                                         BbModel model, List<String> warnings) {
        float[] low = new float[3];
        float[] high = new float[3];
        bounds(cube, low, high);

        float[] from = new float[3];
        float[] to = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            from[axis] = low[axis] - center[axis] + 8.0F;
            to[axis] = high[axis] - center[axis] + 8.0F;
        }
        checkRange(from, warnings);
        checkRange(to, warnings);

        JsonObject faces = new JsonObject();
        for (String faceName : FACE_NAMES) {
            BbCube.Face face = cube.faces.get(faceName);
            if (face == null || face.uvPixels == null || face.textureIndex < 0) {
                continue;
            }
            int index = Math.max(0, Math.min(textureCount - 1, face.textureIndex));
            JsonObject faceJson = new JsonObject();
            JsonArray uv = new JsonArray();
            uv.add(round(face.uvPixels[0] * 16.0F / model.textureWidth));
            uv.add(round(face.uvPixels[1] * 16.0F / model.textureHeight));
            uv.add(round(face.uvPixels[2] * 16.0F / model.textureWidth));
            uv.add(round(face.uvPixels[3] * 16.0F / model.textureHeight));
            faceJson.add("uv", uv);
            faceJson.addProperty("texture", "#tex" + index);
            if (face.rotation != 0) {
                faceJson.addProperty("rotation", face.rotation);
            }
            faces.add(faceName, faceJson);
        }
        if (faces.size() == 0) {
            return null;
        }

        JsonObject elementJson = new JsonObject();
        elementJson.add("from", vec3(from));
        elementJson.add("to", vec3(to));
        elementJson.add("faces", faces);
        return elementJson;
    }

    private static void checkRange(float[] v, List<String> warnings) {
        for (int axis = 0; axis < 3; axis++) {
            if (v[axis] < -16.0F || v[axis] > 32.0F) {
                warnings.add("element coordinate " + v[axis] + " outside vanilla's [-16, 32] range");
            }
        }
    }

    private static JsonArray vec3(float[] v) {
        JsonArray array = new JsonArray();
        array.add(round(v[0]));
        array.add(round(v[1]));
        array.add(round(v[2]));
        return array;
    }

    private static float round(float value) {
        return Math.round(value * 100000.0F) / 100000.0F;
    }
}
