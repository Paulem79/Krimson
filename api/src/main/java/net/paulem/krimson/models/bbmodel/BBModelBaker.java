package net.paulem.krimson.models.bbmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Transforme un {@link BBModelParser.ParsedBBModel} en :
 * 1) les DisplayPart de bind-pose (une par bone), avec un ItemStack pointant
 *    vers le modèle custom généré pour ce bone ({@link #generateItemModelJson}).
 * 2) les AnimationFrame par tick (forward-kinematics complet, cf. javadoc de
 *    classe plus bas pour le détail des conventions d'unités).
 * <p>
 * === Conventions d'unités (À VÉRIFIER EN JEU, cf. constantes ci-dessous) ===
 * - Le pivot d'un bone ("origin" bbmodel) est en unités Blockbench (16=1 bloc)
 *   -> divisé par 16 pour obtenir des blocs.
 * - Les keyframes du canal "position" sont supposées déjà exprimées en BLOCS
 *   (convention Blockbench/Bedrock la plus courante pour l'animation d'un
 *   bone). Si en jeu tes bones se décalent d'un facteur ~16 par rapport à ce
 *   qui est attendu, ajuste {@link #POSITION_ANIM_SCALE}.
 * - Rotation d'élément vs rotation de bone : la rotation D'ÉLÉMENT (dans
 *   "elements[].rotation") est BAKÉE STATIQUEMENT dans le modèle resource pack
 *   (donc PAS animable), limitée par le format vanilla à un seul axe, angle
 *   parmi {-45,-22.5,0,22.5,45}. Seule la rotation du BONE (accumulée via
 *   l'entity transform à chaque tick) est réellement animée — c'est le
 *   fonctionnement standard d'un rig à la Blockbench/Geckolib.
 */
public final class BBModelBaker {

    /** cf. note de classe sur les conventions d'unités du canal "position". */
    private static final float POSITION_ANIM_SCALE = 1.0f;

    private BBModelBaker() {}

    // ============================================================
    //  BIND POSE
    // ============================================================

    public static class BakedPart {
        public String tag;
        public JsonObject itemModelJson;
        public String modelKeySuffix; // ex: "speaker_bone_arm" -> assets/krimson/items/speaker_bone_arm.json
        public Matrix4f bindTransform;
    }

    public static List<BakedPart> bakeBindPose(BBModelParser.ParsedBBModel model, String modelBaseName) {
        // Une clé de texture stable et sans collision par INDEX de texture du
        // bbmodel (indépendamment de son nom d'origine, ex: "the_world.png" ->
        // "the_world_tex0"), utilisée à la fois pour nommer le fichier PNG écrit
        // par le resource pack et pour la référence "krimson:<clé>" dans le model json.
        List<String> textureKeys = new ArrayList<>();
        for (int i = 0; i < model.textures.size(); i++) textureKeys.add(modelBaseName + "_tex" + i);

        List<BakedPart> parts = new ArrayList<>();
        model.forEachBone(bone -> {
            if (!bone.hasGeometry()) return; // bone purement structurel (pas de géométrie propre) -> pas d'entité
            if (!bone.visible) return; // bone masqué dans l'éditeur (ex: bras/jambes "BAM" hors-champ,
            // utilisés seulement par certaines animations d'attaque) -> pas spawné par défaut.
            // cf. javadoc de BBBone.visible pour le détail. Si tu veux QUAND MÊME
            // ces bones dispo pour certaines animations spécifiques, il faudra un
            // système de spawn/despawn par animation (pas géré ici en V1).

            BakedPart part = new BakedPart();
            part.tag = bone.tag();
            part.modelKeySuffix = modelBaseName + "_" + part.tag;
            part.itemModelJson = generateItemModelJson(bone, model.textureWidth, model.textureHeight, textureKeys);

            Vector3f worldPivotBlocks = new Vector3f(bone.pivot).div(16f);
            part.bindTransform = new Matrix4f().translate(worldPivotBlocks);

            parts.add(part);
        });
        return parts;
    }

    /**
     * Génère un item model vanilla (format standard {parent, textures, elements})
     * contenant tous les éléments propres à ce bone, exprimés RELATIVEMENT au
     * pivot du bone (pivot = origine du modèle), pour que la rotation appliquée
     * par l'entity transform tourne bien autour du bon point.
     * <p>
     * Multi-texture : chaque texture du bbmodel devient une variable "tex&lt;i&gt;"
     * dans le bloc "textures", et chaque face référence "#tex&lt;i&gt;" selon son
     * {@link BBElement.Face#textureIndex} d'origine (0 = 1ère texture, etc.).
     */
    public static JsonObject generateItemModelJson(BBBone bone, int texW, int texH, List<String> textureKeys) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated"); // ignoré dès qu'"elements" est présent, gardé pour fallback GUI

        JsonObject textures = new JsonObject();
        for (int i = 0; i < textureKeys.size(); i++) {
            // "block/" est requis : c'est le seul sous-dossier de textures/ que
            // l'atlas des blocs scanne par défaut (atlases/blocks.json vanilla).
            // Une texture placée à la racine de textures/ est ignorée par le
            // stitcher, d'où "Missing textures ... blocks.png:krimson:xxx" même
            // si le fichier PNG existe et est valide.
            textures.addProperty("tex" + i, "krimson:block/" + textureKeys.get(i));
        }
        if (!textureKeys.isEmpty()) textures.addProperty("particle", "krimson:block/" + textureKeys.get(0));
        root.add("textures", textures);

        JsonArray elements = new JsonArray();
        for (BBElement el : bone.ownElements) {
            elements.add(elementToJson(el, bone.pivot, texW, texH, textureKeys.size()));
        }
        root.add("elements", elements);
        return root;
    }

    private static JsonObject elementToJson(BBElement el, Vector3f bonePivot, int texW, int texH, int textureCount) {
        JsonObject o = new JsonObject();

        Vector3f from = new Vector3f(el.from).sub(bonePivot);
        Vector3f to = new Vector3f(el.to).sub(bonePivot);
        clampToVanillaRange(from);
        clampToVanillaRange(to);

        o.add("from", vec3Json(from));
        o.add("to", vec3Json(to));

        AxisAngle rot = toVanillaAxisAngle(el.rotation);
        if (rot != null) {
            JsonObject rotation = new JsonObject();
            Vector3f origin = new Vector3f(el.rotationOrigin).sub(bonePivot);
            rotation.add("origin", vec3Json(origin));
            rotation.addProperty("axis", rot.axis);
            rotation.addProperty("angle", rot.angle);
            o.add("rotation", rotation);
        }

        JsonObject faces = new JsonObject();
        for (Map.Entry<String, BBElement.Face> entry : el.faces.entrySet()) {
            BBElement.Face f = entry.getValue();
            if (f.textureIndex < 0 || f.uvPixels == null) continue;
            JsonObject fo = new JsonObject();
            JsonArray uv = new JsonArray();
            // conversion pixels (0..resolution) -> unités modèle (0..16)
            uv.add(f.uvPixels[0] * 16f / texW);
            uv.add(f.uvPixels[1] * 16f / texH);
            uv.add(f.uvPixels[2] * 16f / texW);
            uv.add(f.uvPixels[3] * 16f / texH);
            fo.add("uv", uv);
            int idx = Math.max(0, Math.min(textureCount - 1, f.textureIndex));
            fo.addProperty("texture", "#tex" + idx);
            faces.add(entry.getKey(), fo);
        }
        o.add("faces", faces);

        return o;
    }

    private static void clampToVanillaRange(Vector3f v) {
        v.x = Math.max(-16f, Math.min(32f, v.x));
        v.y = Math.max(-16f, Math.min(32f, v.y));
        v.z = Math.max(-16f, Math.min(32f, v.z));
    }

    private record AxisAngle(String axis, float angle) {}

    /**
     * Le format vanilla n'autorise qu'un seul axe de rotation par élément, avec
     * un angle parmi {-45,-22.5,0,22.5,45}. On prend l'axe dominant du vecteur
     * de rotation Blockbench (qui lui autorise 3 axes libres) et on arrondit
     * au pas vanilla le plus proche. Si les 2 autres axes ne sont pas ~0,
     * c'est une PERTE DE FIDÉLITÉ assumée (log un warning côté appelant si besoin).
     */
    static AxisAngle toVanillaAxisAngle(Vector3f rotationDegrees) {
        float ax = Math.abs(rotationDegrees.x), ay = Math.abs(rotationDegrees.y), az = Math.abs(rotationDegrees.z);
        if (ax < 0.01f && ay < 0.01f && az < 0.01f) return null;

        String axis; float value;
        if (ax >= ay && ax >= az) { axis = "x"; value = rotationDegrees.x; }
        else if (ay >= ax && ay >= az) { axis = "y"; value = rotationDegrees.y; }
        else { axis = "z"; value = rotationDegrees.z; }

        float[] allowed = {-45f, -22.5f, 0f, 22.5f, 45f};
        float snapped = allowed[0];
        float bestDist = Float.MAX_VALUE;
        for (float a : allowed) {
            float d = Math.abs(a - value);
            if (d < bestDist) { bestDist = d; snapped = a; }
        }
        if (snapped == 0f) return null;
        return new AxisAngle(axis, snapped);
    }

    private static JsonArray vec3Json(Vector3f v) {
        JsonArray a = new JsonArray();
        a.add(v.x); a.add(v.y); a.add(v.z);
        return a;
    }

    // ============================================================
    //  ANIMATIONS (forward kinematics, tick par tick, linéaire uniquement)
    // ============================================================

    public record BakedFrame(String boneTag, Matrix4f transformation) {}

    public static Map<String, Map<Integer, List<BakedFrame>>> bakeAnimations(BBModelParser.ParsedBBModel model) {
        Map<String, Map<Integer, List<BakedFrame>>> result = new LinkedHashMap<>();

        for (BBAnimation anim : model.animations) {
            Map<Integer, List<BakedFrame>> ticks = new TreeMap<>();
            int totalTicks = anim.totalTicks();

            for (int tick = 0; tick <= totalTicks; tick++) {
                double t = tick / 20.0;
                List<BakedFrame> frames = new ArrayList<>();

                for (BBBone root : model.rootBones) {
                    bakeBoneAtTime(root, anim, t, new Vector3f(), new Quaternionf(), frames);
                }
                ticks.put(tick, frames);
            }
            result.put(anim.name, ticks);
        }
        return result;
    }

    /**
     * Calcule récursivement la position/rotation MONDE d'un bone à l'instant t
     * (forward kinematics complet, nécessaire car les display entities ne sont
     * pas montées en Passengers et doivent donc recevoir leur transform monde
     * complet à chaque tick).
     */
    private static void bakeBoneAtTime(BBBone bone, BBAnimation anim, double t,
                                       Vector3f parentWorldPosBlocks, Quaternionf parentWorldRot,
                                       List<BakedFrame> out) {
        BBAnimation.Animator animator = anim.animators.get(bone.uuid);

        Vector3f localPosOffset = animator != null ? sampleVec3(animator.channel("position"), t) : new Vector3f();
        Vector3f localRotDeg = animator != null ? sampleVec3(animator.channel("rotation"), t) : new Vector3f();
        localRotDeg.add(bone.bindRotation);

        // Canal "scale" : contrairement à position/rotation, la valeur par défaut
        // en absence de keyframe est (1,1,1) et non (0,0,0) — sinon le bone
        // disparaît (scale nul) dès qu'il n'a pas de canal scale animé.
        Vector3f localScale = animator != null && !animator.channel("scale").isEmpty()
                ? sampleVec3(animator.channel("scale"), t)
                : new Vector3f(1f, 1f, 1f);

        Vector3f pivotRelToParentBlocks = bone.parent != null
                ? new Vector3f(bone.pivot).sub(bone.parent.pivot).div(16f)
                : new Vector3f(bone.pivot).div(16f);

        Vector3f localOffsetBlocks = new Vector3f(localPosOffset).mul(POSITION_ANIM_SCALE / 16f);

        Vector3f rotatedOffset = parentWorldRot.transform(new Vector3f(pivotRelToParentBlocks).add(localOffsetBlocks), new Vector3f());
        Vector3f worldPosBlocks = new Vector3f(parentWorldPosBlocks).add(rotatedOffset);

        Quaternionf localRot = eulerDegreesToQuat(localRotDeg);
        Quaternionf worldRot = new Quaternionf(parentWorldRot).mul(localRot);

        if (bone.hasGeometry()) {
            Matrix4f transform = new Matrix4f().translate(worldPosBlocks).rotate(worldRot).scale(localScale);
            out.add(new BakedFrame(bone.tag(), transform));
        }

        for (BBBone child : bone.children) {
            bakeBoneAtTime(child, anim, t, worldPosBlocks, worldRot, out);
        }
    }

    private static Quaternionf eulerDegreesToQuat(Vector3f degrees) {
        return new Quaternionf().rotateXYZ(
                (float) Math.toRadians(degrees.x),
                (float) Math.toRadians(degrees.y),
                (float) Math.toRadians(degrees.z));
    }

    /** Interpolation linéaire entre les 2 keyframes encadrant t (V1 : pas de bezier/catmullrom). */
    private static Vector3f sampleVec3(List<BBAnimation.Keyframe> keyframes, double t) {
        if (keyframes.isEmpty()) return new Vector3f();
        if (t <= keyframes.get(0).time) {
            BBAnimation.Keyframe k = keyframes.get(0);
            return new Vector3f(k.x, k.y, k.z);
        }
        BBAnimation.Keyframe last = keyframes.get(keyframes.size() - 1);
        if (t >= last.time) return new Vector3f(last.x, last.y, last.z);

        for (int i = 0; i < keyframes.size() - 1; i++) {
            BBAnimation.Keyframe a = keyframes.get(i);
            BBAnimation.Keyframe b = keyframes.get(i + 1);
            if (t >= a.time && t <= b.time) {
                float f = b.time > a.time ? (float) ((t - a.time) / (b.time - a.time)) : 0f;
                return new Vector3f(
                        a.x + (b.x - a.x) * f,
                        a.y + (b.y - a.y) * f,
                        a.z + (b.z - a.z) * f
                );
            }
        }
        return new Vector3f(last.x, last.y, last.z);
    }

    // ============================================================
    //  ItemStack utilitaire pour le bind-pose (custom model, cf. ResourcePack.kt)
    // ============================================================

    public static ItemStack buildDisplayItem(org.bukkit.NamespacedKey itemModelKey) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(itemModelKey); // API 1.21.4+ (component item_model)
        stack.setItemMeta(meta);
        return stack;
    }
}