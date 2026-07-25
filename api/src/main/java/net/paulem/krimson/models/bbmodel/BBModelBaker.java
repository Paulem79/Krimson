package net.paulem.krimson.models.bbmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Transforme un {@link BBModelParser.ParsedBBModel} en :
 * 1) les {@link BakedPart} de bind pose (une par bone porteur de géométrie), avec
 *    l'item model custom généré pour ce bone ({@link #generateItemModelJson}) ;
 * 2) les {@link BakedFrame} par tick, via une cinématique directe complète.
 *
 * <h2>Conventions d'unités</h2>
 * Tout le calcul de hiérarchie se fait en unités BLOCKBENCH (16 = 1 bloc) et la
 * conversion en blocs a lieu une seule fois, dans {@link #toDisplayTransform}.
 * Les keyframes du canal "position" sont dans la MÊME unité que les pivots
 * (unités Blockbench), elles s'additionnent donc directement au pivot sans
 * facteur d'échelle intermédiaire.
 *
 * <h2>Pourquoi une chaîne de Matrix4f et pas un couple (quaternion, vecteur)</h2>
 * Une FK à base de quaternion + vecteur d'échelle séparés ne peut pas représenter
 * un parent à échelle NON UNIFORME suivi d'un enfant qui tourne : il faudrait
 * appliquer {@code R_parent * S_parent * R_enfant}, or un cumul de quaternions
 * donne {@code R_parent * R_enfant} et applique les échelles à part. Le cas se
 * produit réellement dans "the_world" : le bone {@code kick_barrage} est mis à
 * l'échelle (1 ; 1,3 ; 1) pendant {@code KickBarrage} et ses enfants
 * {@code One..Four} tournent. Une chaîne de {@link Matrix4f} propage
 * rotation ET échelle correctement, gratuitement.
 *
 * <h2>Rotation d'élément vs rotation de bone</h2>
 * Inchangé : la rotation D'ÉLÉMENT est bakée statiquement dans le resource pack
 * (limitée par le format vanilla à un axe et un angle parmi {-45,-22.5,0,22.5,45},
 * cf. {@link #toVanillaAxisAngle}). Seule la rotation du BONE est animée.
 */
public final class BBModelBaker {

    /**
     * Compensation du centrage propre au rendu {@code item_display} : le client
     * dessine le modèle CENTRÉ sur l'entité, donc la coordonnée modèle (0,0,0)
     * tombe à -0,5 bloc sur chaque axe, et non sur l'origine de l'entité.
     * <p>
     * C'est la cause du décalage constant de 0,5 bloc en bind pose, et surtout
     * des parts qui « partent en orbite » dès qu'un bone tourne : sans cette
     * compensation, le décalage de -0,5 est lui aussi tourné par la matrice,
     * ce qui écarte la géométrie de son pivot de jusqu'à 0,87 bloc
     * (= |(0,5 ; 0,5 ; 0,5)|).
     * <p>
     * Si un jour le rendu s'avérait ancré différemment sur ta build, c'est LA
     * seule constante à changer — la géométrie du pack reste inchangée.
     */
    private static final float ITEM_DISPLAY_CENTER_OFFSET = 0.5f;

    /** Unités Blockbench par bloc. */
    private static final float UNITS_PER_BLOCK = 16f;

    private BBModelBaker() {}

    // ============================================================
    //  BIND POSE
    // ============================================================

    public static class BakedPart {
        public String tag;
        public JsonObject itemModelJson;
        public String modelKeySuffix; // ex: "the_world_bone_head" -> assets/krimson/items/the_world_bone_head.json
        public Matrix4f bindTransform;
        /**
         * Reflète le flag "visibility" du groupe Blockbench. Les bones masqués
         * SONT désormais bakés (leur item model existe donc dans le pack) et
         * spawnés, mais parqués invisibles : c'est ce qui permet aux animations
         * {@code Barrage} / {@code KickBarrage} de les faire apparaître, ce qui
         * était impossible quand ils étaient simplement ignorés ici.
         */
        public boolean visibleByDefault;
    }

    public static List<BakedPart> bakeBindPose(BBModelParser.ParsedBBModel model, String modelBaseName) {
        // Une clé de texture stable et sans collision par INDEX de texture du
        // bbmodel (indépendamment de son nom d'origine, ex: "the_world.png" ->
        // "the_world_tex0"), utilisée à la fois pour nommer le fichier PNG écrit
        // par le resource pack et pour la référence "krimson:<clé>" dans le model json.
        List<String> textureKeys = new ArrayList<>();
        for (int i = 0; i < model.textures.size(); i++) textureKeys.add(modelBaseName + "_tex" + i);

        // La bind pose est la même chaîne de matrices que les frames animées,
        // avec une animation nulle : une seule implémentation, donc aucun risque
        // de divergence entre pose de repos et premier tick d'animation.
        Map<String, Matrix4f> worldByBone = solveWorldMatrices(model, null, 0.0);

        List<BakedPart> parts = new ArrayList<>();
        model.forEachBone(bone -> {
            if (!bone.hasGeometry()) return; // bone purement structurel -> pas d'entité

            BakedPart part = new BakedPart();
            part.tag = bone.tag();
            part.modelKeySuffix = modelBaseName + "_" + part.tag;
            part.itemModelJson = generateItemModelJson(bone, model.textureWidth, model.textureHeight, textureKeys);
            part.visibleByDefault = bone.visible;

            Matrix4f world = worldByBone.get(bone.uuid);
            part.bindTransform = toDisplayTransform(world != null ? world : new Matrix4f(), bone.pivot);

            parts.add(part);
        });
        return parts;
    }

    /**
     * Génère un item model vanilla (format standard {parent, textures, elements})
     * contenant tous les éléments propres à ce bone, exprimés RELATIVEMENT au
     * pivot du bone, pour que la rotation appliquée par l'entity transform tourne
     * bien autour du bon point.
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
        if (el.inflate != 0f) {
            from.sub(el.inflate, el.inflate, el.inflate);
            to.add(el.inflate, el.inflate, el.inflate);
        }
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
    //  CINÉMATIQUE DIRECTE
    // ============================================================

    /**
     * Matrice monde de chaque bone, en unités Blockbench, à l'instant {@code time}.
     * <p>
     * Par bone : {@code monde = parent * T(pivot + position) * Rz * Ry * Rx * S * T(-pivot)}.
     * L'ordre Z puis Y puis X est celui de Blockbench et du {@code ModelPart}
     * vanilla ; l'ancienne implémentation utilisait {@code Quaternionf.rotateXYZ},
     * soit l'ordre inverse, ce qui vrillait tout bone tournant sur plusieurs axes
     * à la fois (jusqu'à 1,63 bloc d'écart sur "the_world").
     *
     * @param animation animation à échantillonner, ou {@code null} pour la bind pose
     */
    public static Map<String, Matrix4f> solveWorldMatrices(BBModelParser.ParsedBBModel model,
                                                           BBAnimation animation, double time) {
        Map<String, Matrix4f> out = new LinkedHashMap<>();
        for (BBBone root : model.rootBones) {
            solveBone(root, animation, time, new Matrix4f(), out);
        }
        return out;
    }

    private static void solveBone(BBBone bone, BBAnimation animation, double time,
                                  Matrix4f parent, Map<String, Matrix4f> out) {
        Vector3f position = new Vector3f();
        Vector3f rotation = new Vector3f();
        Vector3f scale = new Vector3f(1f, 1f, 1f);

        BBAnimation.Animator animator = animation != null ? animation.animators.get(bone.uuid) : null;
        if (animator != null) {
            sample(animator.channel(BBAnimation.Channel.POSITION), time, BBAnimation.Channel.POSITION, position);
            sample(animator.channel(BBAnimation.Channel.ROTATION), time, BBAnimation.Channel.ROTATION, rotation);
            sample(animator.channel(BBAnimation.Channel.SCALE), time, BBAnimation.Channel.SCALE, scale);
        }
        // La rotation de bind pose s'ajoute à la rotation animée, comme dans Blockbench.
        rotation.add(bone.bindRotation);

        Matrix4f world = new Matrix4f(parent)
                .translate(bone.pivot.x + position.x, bone.pivot.y + position.y, bone.pivot.z + position.z)
                .rotateZ((float) Math.toRadians(rotation.z))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateX((float) Math.toRadians(rotation.x))
                .scale(scale)
                .translate(-bone.pivot.x, -bone.pivot.y, -bone.pivot.z);

        out.put(bone.uuid, world);

        for (BBBone child : bone.children) {
            solveBone(child, animation, time, world, out);
        }
    }

    /**
     * Convertit la matrice monde d'un bone (unités Blockbench) en transformation
     * d'{@code ItemDisplay} (blocs).
     * <p>
     * La géométrie du pack étant bakée relativement au pivot, le client rend le
     * sommet {@code v} à {@code (v - pivot)/16 - 0,5} bloc. On veut qu'il tombe à
     * {@code monde * v / 16}. En gardant la partie linéaire de la matrice monde et
     * en posant sa translation à {@code monde * pivot / 16}, il reste juste à
     * annuler le centrage avec un {@code translate(+0,5)} appliqué AVANT le reste
     * — d'où l'ordre des appels ci-dessous.
     */
    public static Matrix4f toDisplayTransform(Matrix4f boneWorld, Vector3f bonePivot) {
        Vector3f pivotBlocks = boneWorld.transformPosition(new Vector3f(bonePivot)).div(UNITS_PER_BLOCK);

        Matrix4f out = new Matrix4f(boneWorld);        // conserve rotation * échelle accumulées
        out.setTranslation(pivotBlocks);               // translation en blocs, pas en unités Blockbench
        out.translate(ITEM_DISPLAY_CENTER_OFFSET, ITEM_DISPLAY_CENTER_OFFSET, ITEM_DISPLAY_CENTER_OFFSET);
        return out;
    }

    // ============================================================
    //  ÉCHANTILLONNAGE DES KEYFRAMES
    // ============================================================

    /** Écrit dans {@code target} la valeur du canal à l'instant {@code time}. */
    private static void sample(List<BBAnimation.Keyframe> keyframes, double time,
                               BBAnimation.Channel channel, Vector3f target) {
        int count = keyframes.size();
        if (count == 0) {
            target.set(channel.restValue);
            return;
        }
        BBAnimation.Keyframe first = keyframes.get(0);
        if (count == 1 || time <= first.time) {
            target.set(first.x, first.y, first.z);
            return;
        }
        BBAnimation.Keyframe last = keyframes.get(count - 1);
        if (time >= last.time) {
            target.set(last.x, last.y, last.z);
            return;
        }

        int index = 0;
        while (index < count - 1 && keyframes.get(index + 1).time <= time) index++;

        BBAnimation.Keyframe from = keyframes.get(index);
        BBAnimation.Keyframe to = keyframes.get(index + 1);
        double span = to.time - from.time;
        float progress = span <= 1.0E-9 ? 0f : (float) ((time - from.time) / span);

        switch (from.interpolation) {
            case STEP -> target.set(from.x, from.y, from.z);
            case CATMULLROM -> {
                // Catmull-Rom uniforme sur les 4 keyframes encadrantes, en
                // dupliquant les extrémités quand on est au bord de la timeline.
                BBAnimation.Keyframe before = index > 0 ? keyframes.get(index - 1) : from;
                BBAnimation.Keyframe after = index + 2 < count ? keyframes.get(index + 2) : to;
                target.set(
                        catmullRom(before.x, from.x, to.x, after.x, progress),
                        catmullRom(before.y, from.y, to.y, after.y, progress),
                        catmullRom(before.z, from.z, to.z, after.z, progress));
            }
            default -> target.set(
                    from.x + (to.x - from.x) * progress,
                    from.y + (to.y - from.y) * progress,
                    from.z + (to.z - from.z) * progress);
        }
    }

    /** Catmull-Rom uniforme, équivalent au mode "smooth" de Blockbench. */
    static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    // ============================================================
    //  ANIMATIONS BAKÉES PAR TICK
    // ============================================================

    public record BakedFrame(String boneTag, Matrix4f transformation) {}

    /**
     * Résultat du bake d'une animation : les frames par tick, le mode de bouclage
     * et les tags de parts que cette animation doit rendre visibles.
     */
    public record BakedAnimation(Map<Integer, List<BakedFrame>> ticks,
                                 BBAnimation.LoopMode loopMode,
                                 Set<String> revealedPartTags) {}

    public static Map<String, BakedAnimation> bakeAnimations(BBModelParser.ParsedBBModel model) {
        Map<String, BakedAnimation> result = new LinkedHashMap<>();

        for (BBAnimation anim : model.animations) {
            BBAnimation.LoopMode loopMode = anim.loop();
            int totalTicks = anim.totalTicks();

            // En bouclage, la dernière frame est celle qui PRÉCÈDE le retour à
            // zéro : la baker sur [0, totalTicks] inclus rejouerait deux fois la
            // même pose au raccord (t = length et t = 0 sont identiques dans un
            // cycle bien construit), ce qui produit un micro-arrêt d'un tick.
            boolean looping = loopMode == BBAnimation.LoopMode.LOOP;
            int frameCount = looping ? totalTicks : totalTicks + 1;

            Map<Integer, List<BakedFrame>> ticks = new TreeMap<>();
            for (int tick = 0; tick < frameCount; tick++) {
                // En boucle, on répartit les frames sur la durée exacte de
                // l'animation pour que la période reste celle voulue par l'auteur ;
                // sinon on reste sur la grille 20 Hz et on clamp sur la fin.
                double time = looping
                        ? anim.lengthSeconds * ((double) tick / frameCount)
                        : Math.min(anim.lengthSeconds, tick / 20.0);

                Map<String, Matrix4f> worldByBone = solveWorldMatrices(model, anim, time);

                List<BakedFrame> frames = new ArrayList<>();
                model.forEachBone(bone -> {
                    if (!bone.hasGeometry()) return;
                    Matrix4f world = worldByBone.get(bone.uuid);
                    if (world == null) return;
                    frames.add(new BakedFrame(bone.tag(), toDisplayTransform(world, bone.pivot)));
                });
                ticks.put(tick, frames);
            }

            result.put(anim.name, new BakedAnimation(ticks, loopMode, revealedPartTags(model, anim)));
        }
        return result;
    }

    /**
     * Tags des parts masquées par défaut que cette animation doit faire apparaître.
     * <p>
     * On ne remonte volontairement PAS aux descendants d'un bone animé : la
     * plupart des animations bougent un bone haut dans la hiérarchie (par ex.
     * {@code body}), ce qui ferait alors apparaître tous les bones masqués du
     * modèle. Sur "the_world" les bones masqués porteurs de géométrie sont tous
     * des feuilles, donc l'égalité stricte donne exactement le bon jeu :
     * {@code Barrage}/{@code BarrageCharge} -> les 6 bras "BAM",
     * {@code KickBarrage} -> les 4 jambes, tout le reste -> rien.
     */
    private static Set<String> revealedPartTags(BBModelParser.ParsedBBModel model, BBAnimation anim) {
        Set<String> moved = anim.movedBoneUuids();
        Set<String> tags = new LinkedHashSet<>();
        model.forEachBone(bone -> {
            if (!bone.visible && bone.hasGeometry() && moved.contains(bone.uuid)) {
                tags.add(bone.tag());
            }
        });
        return Collections.unmodifiableSet(tags);
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
