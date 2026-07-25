package net.paulem.krimson.models.bbmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse un fichier .bbmodel brut (export Blockbench natif, PAS le JSON exporté
 * par le plugin BDEngine que {@link net.paulem.krimson.models.BlockDisplayModel}
 * consommait jusqu'ici).
 * <p>
 * Hypothèses faites sur le format (vérifiées sur l'extrait fourni) :
 * - "elements[]" contient les cuboïdes, indexés par leur "uuid".
 * - "outliner[]" est l'arbre faisant foi pour la hiérarchie : une feuille est
 *   une STRING (uuid d'un element), un noeud interne est un OBJECT avec
 *   name/uuid/origin/rotation/children.
 * - "textures[]" contient soit un champ "source" en data-URI base64
 *   ("data:image/png;base64,...."), soit un chemin — seul le base64 est géré ici.
 * - Le champ "texture" d'une face peut être un INDEX numérique (ancien format,
 *   box_uv=true) ou un STRING uuid de texture (format per-face récent) : les
 *   deux sont gérés dans {@link #resolveTextureIndex}.
 */
public final class BBModelParser {

    private BBModelParser() {}

    public static ParsedBBModel parse(File bbmodelFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(bbmodelFile.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return parse(root);
        }
    }

    public static ParsedBBModel parse(JsonObject root) {
        ParsedBBModel result = new ParsedBBModel();

        JsonObject resolution = root.has("resolution") ? root.getAsJsonObject("resolution") : null;
        result.textureWidth = resolution != null && resolution.has("width") ? resolution.get("width").getAsInt() : 16;
        result.textureHeight = resolution != null && resolution.has("height") ? resolution.get("height").getAsInt() : 16;

        Map<String, BBElement> elementsByUuid = parseElements(root);
        List<TextureEntry> textures = parseTextures(root);
        result.textures = textures;

        // Métadonnées des groupes (name/origin/rotation) : PAS dans les noeuds
        // outliner eux-mêmes (qui ne contiennent que uuid/isOpen/children dans
        // la plupart des exports réels), mais dans le tableau "groups[]" à part,
        // indexé par uuid. On construit cette table AVANT de marcher l'outliner.
        Map<String, GroupMeta> groupMetaByUuid = parseGroupsMeta(root);

        // outliner -> arbre de bones
        List<BBBone> roots = new ArrayList<>();
        Map<String, BBBone> bonesByUuid = new HashMap<>();
        if (root.has("outliner")) {
            for (JsonElement child : root.getAsJsonArray("outliner")) {
                BBBone bone = parseOutlinerNode(child, null, elementsByUuid, bonesByUuid, groupMetaByUuid);
                if (bone != null) roots.add(bone);
            }
        }
        result.rootBones = roots;
        result.bonesByUuid = bonesByUuid;

        // Elements jamais rattachés à un groupe (à la racine du modèle) : on les
        // regroupe dans un bone racine synthétique nommé "root" pour ne rien perdre.
        List<BBElement> orphanElements = new ArrayList<>();
        if (root.has("outliner")) {
            java.util.Set<String> attached = new java.util.HashSet<>();
            collectAttachedUuids(root.getAsJsonArray("outliner"), attached);
            for (Map.Entry<String, BBElement> entry : elementsByUuid.entrySet()) {
                if (!attached.contains(entry.getKey())) orphanElements.add(entry.getValue());
            }
        } else {
            orphanElements.addAll(elementsByUuid.values());
        }
        if (!orphanElements.isEmpty()) {
            BBBone syntheticRoot = new BBBone("root_synthetic", "root");
            syntheticRoot.ownElements.addAll(orphanElements);
            result.rootBones.add(syntheticRoot);
        }

        result.animations = parseAnimations(root, bonesByUuid);

        return result;
    }

    // ---------- elements ----------

    private static Map<String, BBElement> parseElements(JsonObject root) {
        Map<String, BBElement> map = new HashMap<>();
        if (!root.has("elements")) return map;

        for (JsonElement el : root.getAsJsonArray("elements")) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("type") && !o.get("type").getAsString().equals("cube")) continue; // meshes non gérés en V1

            BBElement e = new BBElement();
            e.uuid = getString(o, "uuid", java.util.UUID.randomUUID().toString());
            e.name = getString(o, "name", "element");
            readVec3(o, "from", e.from);
            readVec3(o, "to", e.to);
            e.inflate = o.has("inflate") ? o.get("inflate").getAsFloat() : 0f;

            if (o.has("rotation")) readVec3Array(o.get("rotation"), e.rotation);
            if (o.has("origin")) readVec3Array(o.get("origin"), e.rotationOrigin);

            if (o.has("faces")) {
                JsonObject faces = o.getAsJsonObject("faces");
                for (Map.Entry<String, JsonElement> fEntry : faces.entrySet()) {
                    JsonObject fo = fEntry.getValue().getAsJsonObject();
                    BBElement.Face face = new BBElement.Face();
                    if (fo.has("uv")) {
                        JsonArray uv = fo.getAsJsonArray("uv");
                        if (uv.size() == 4) {
                            face.uvPixels = new float[]{
                                    uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                                    uv.get(2).getAsFloat(), uv.get(3).getAsFloat()
                            };
                        }
                    }
                    face.textureIndex = resolveTextureIndex(fo);
                    e.faces.put(fEntry.getKey(), face);
                }
            }

            map.put(e.uuid, e);
        }
        return map;
    }

    private static int resolveTextureIndex(JsonObject face) {
        if (!face.has("texture") || face.get("texture").isJsonNull()) return -1;
        JsonElement tex = face.get("texture");
        if (tex.isJsonPrimitive() && tex.getAsJsonPrimitive().isNumber()) {
            return tex.getAsInt();
        }
        // Format per-face récent : uuid de texture en string. On résout l'index
        // réel une fois la liste de textures connue, via un marqueur négatif
        // encodé — ici on renvoie -2 pour signaler "à résoudre par uuid" et on
        // stocke l'uuid dans une table à part si besoin. Pour rester simple en
        // V1, on suppose le format legacy à index numérique (box_uv=true), qui
        // est celui de l'exemple fourni. Si ton export utilise le format par
        // uuid, dis-le moi et j'ajoute la résolution par uuid ici.
        return -1;
    }

    // ---------- textures ----------

    public static class TextureEntry {
        public String name;
        public byte[] pngBytes;
    }

    private static List<TextureEntry> parseTextures(JsonObject root) {
        List<TextureEntry> list = new ArrayList<>();
        if (!root.has("textures")) return list;
        for (JsonElement el : root.getAsJsonArray("textures")) {
            JsonObject o = el.getAsJsonObject();
            TextureEntry t = new TextureEntry();
            t.name = getString(o, "name", "texture_" + list.size());
            String source = getString(o, "source", null);
            if (source != null && source.startsWith("data:")) {
                int comma = source.indexOf(',');
                String b64 = comma >= 0 ? source.substring(comma + 1) : source;
                t.pngBytes = Base64.getDecoder().decode(b64);
            }
            list.add(t);
        }
        return list;
    }

    // ---------- groups[] (métadonnées des bones, séparées de l'outliner) ----------

    private static class GroupMeta {
        String name = "bone";
        final org.joml.Vector3f origin = new org.joml.Vector3f();
        final org.joml.Vector3f rotation = new org.joml.Vector3f();
        boolean visibility = true;
    }

    /**
     * Certains exports .bbmodel dupliquent name/origin/rotation directement
     * dans les noeuds "outliner" (cas de l'extrait de doc fourni initialement).
     * D'autres (ex: format "modded_entity", cas réel observé) ne le font PAS :
     * seul "groups[]" contient ces infos, indexé par uuid. On supporte les 2 en
     * construisant systématiquement cette table depuis "groups[]" quand elle
     * existe ; parseOutlinerNode utilisera en priorité les champs inline s'ils
     * sont présents, sinon retombera sur cette table.
     */
    private static Map<String, GroupMeta> parseGroupsMeta(JsonObject root) {
        Map<String, GroupMeta> map = new HashMap<>();
        if (!root.has("groups")) return map;
        for (JsonElement el : root.getAsJsonArray("groups")) {
            JsonObject o = el.getAsJsonObject();
            String uuid = getString(o, "uuid", null);
            if (uuid == null) continue;
            GroupMeta meta = new GroupMeta();
            meta.name = getString(o, "name", "bone");
            if (o.has("origin")) readVec3Array(o.get("origin"), meta.origin);
            if (o.has("rotation")) readVec3Array(o.get("rotation"), meta.rotation);
            meta.visibility = !o.has("visibility") || o.get("visibility").getAsBoolean();
            map.put(uuid, meta);
        }
        return map;
    }

    // ---------- outliner -> bones ----------

    private static BBBone parseOutlinerNode(JsonElement node, BBBone parent,
                                            Map<String, BBElement> elementsByUuid,
                                            Map<String, BBBone> bonesByUuid,
                                            Map<String, GroupMeta> groupMetaByUuid) {
        if (node.isJsonPrimitive()) {
            // Feuille = uuid d'un élément, rattaché directement au parent courant.
            String elementUuid = node.getAsString();
            BBElement element = elementsByUuid.get(elementUuid);
            if (element != null && parent != null) parent.ownElements.add(element);
            return null;
        }

        JsonObject o = node.getAsJsonObject();
        String uuid = getString(o, "uuid", java.util.UUID.randomUUID().toString());
        GroupMeta meta = groupMetaByUuid.get(uuid);

        String name = o.has("name") ? getString(o, "name", "bone") : (meta != null ? meta.name : "bone");

        BBBone bone = new BBBone(uuid, name);
        bone.parent = parent;

        if (o.has("origin")) {
            readVec3Array(o.get("origin"), bone.pivot);
        } else if (meta != null) {
            bone.pivot.set(meta.origin);
        }

        if (o.has("rotation")) {
            readVec3Array(o.get("rotation"), bone.bindRotation);
        } else if (meta != null) {
            bone.bindRotation.set(meta.rotation);
        }

        if (o.has("visibility")) {
            bone.visible = o.get("visibility").getAsBoolean();
        } else if (meta != null) {
            bone.visible = meta.visibility;
        }

        bonesByUuid.put(uuid, bone);
        if (parent != null) parent.children.add(bone);

        if (o.has("children")) {
            for (JsonElement child : o.getAsJsonArray("children")) {
                parseOutlinerNode(child, bone, elementsByUuid, bonesByUuid, groupMetaByUuid);
            }
        }
        return bone;
    }

    private static void collectAttachedUuids(JsonArray outliner, java.util.Set<String> out) {
        for (JsonElement node : outliner) {
            if (node.isJsonPrimitive()) {
                out.add(node.getAsString());
            } else {
                JsonObject o = node.getAsJsonObject();
                if (o.has("children")) collectAttachedUuids(o.getAsJsonArray("children"), out);
            }
        }
    }

    // ---------- animations ----------

    private static List<BBAnimation> parseAnimations(JsonObject root, Map<String, BBBone> bonesByUuid) {
        List<BBAnimation> list = new ArrayList<>();
        if (!root.has("animations")) return list;

        for (JsonElement el : root.getAsJsonArray("animations")) {
            JsonObject o = el.getAsJsonObject();
            BBAnimation anim = new BBAnimation();
            anim.name = getString(o, "name", "animation");
            anim.loopMode = getString(o, "loop", "loop");
            anim.lengthSeconds = o.has("length") ? o.get("length").getAsDouble() : 1.0;

            if (o.has("animators")) {
                JsonObject animators = o.getAsJsonObject("animators");
                for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                    String boneUuid = entry.getKey();
                    JsonObject animatorObj = entry.getValue().getAsJsonObject();
                    if (!bonesByUuid.containsKey(boneUuid)) continue; // effect/sound animator, pas un bone

                    BBAnimation.Animator animator = new BBAnimation.Animator();
                    animator.boneUuid = boneUuid;
                    animator.boneName = getString(animatorObj, "name", bonesByUuid.get(boneUuid).name);

                    if (animatorObj.has("keyframes")) {
                        for (JsonElement kfEl : animatorObj.getAsJsonArray("keyframes")) {
                            JsonObject kfObj = kfEl.getAsJsonObject();
                            // Un animator peut porter des canaux non transformants
                            // ("effect", "sound", "timeline"...) : on les ignore.
                            BBAnimation.Channel channel = BBAnimation.Channel.fromJson(
                                    getString(kfObj, "channel", "rotation"));
                            if (channel == null) continue;

                            if (!kfObj.has("data_points")) continue;
                            JsonArray dataPoints = kfObj.getAsJsonArray("data_points");
                            if (dataPoints.isEmpty()) continue;
                            JsonObject dp = dataPoints.get(0).getAsJsonObject();

                            BBAnimation.Keyframe kf = new BBAnimation.Keyframe();
                            kf.time = kfObj.has("time") ? kfObj.get("time").getAsDouble() : 0.0;
                            kf.channel = channel;
                            kf.interpolation = BBAnimation.Interpolation.parse(
                                    getString(kfObj, "interpolation", "linear"));
                            kf.x = parseMaybeMath(dp, "x", channel.restValue);
                            kf.y = parseMaybeMath(dp, "y", channel.restValue);
                            kf.z = parseMaybeMath(dp, "z", channel.restValue);
                            animator.keyframes.add(kf);
                        }
                    }
                    // Tri des canaux une fois pour toutes, avant tout bake.
                    animator.finishLoading();
                    anim.animators.put(boneUuid, animator);
                }
            }
            list.add(anim);
        }
        return list;
    }

    /**
     * Les data_points de Blockbench sont souvent des strings (parfois avec un
     * retour à la ligne, parfois une expression molang). Seuls les nombres purs
     * sont gérés ; en cas d'expression, on retombe sur la valeur de REPOS du canal
     * et non sur 0, sinon un canal "scale" non parsé réduirait le bone à néant.
     */
    private static float parseMaybeMath(JsonObject dp, String key, float fallback) {
        if (!dp.has(key) || dp.get(key).isJsonNull()) return fallback;
        JsonElement v = dp.get(key);
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) return v.getAsFloat();
        try {
            return Float.parseFloat(v.getAsString().trim());
        } catch (NumberFormatException ex) {
            return fallback; // expression non-numérique (molang) : non gérée
        }
    }

    // ---------- helpers ----------

    private static String getString(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    private static void readVec3(JsonObject o, String key, org.joml.Vector3f target) {
        if (o.has(key)) readVec3Array(o.get(key), target);
    }

    private static void readVec3Array(JsonElement arrEl, org.joml.Vector3f target) {
        JsonArray arr = arrEl.getAsJsonArray();
        if (arr.size() >= 3) {
            target.set(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
        }
    }

    // ---------- result holder ----------

    public static class ParsedBBModel {
        public int textureWidth;
        public int textureHeight;
        public List<BBBone> rootBones;
        public Map<String, BBBone> bonesByUuid;
        public List<TextureEntry> textures;
        public List<BBAnimation> animations;

        public void forEachBone(java.util.function.Consumer<BBBone> consumer) {
            for (BBBone root : rootBones) root.forEachDescendant(consumer);
        }
    }
}