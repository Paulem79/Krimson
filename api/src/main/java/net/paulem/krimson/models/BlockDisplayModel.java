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
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.paulem.krimson.models.bbmodel.*;
import java.io.File;
import java.util.stream.Collectors;

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

    /**
     * Mode de bouclage par animation, tel que déclaré dans le .bbmodel. Absent
     * pour les modèles legacy (BDEngine / commande vanilla), qui conservent alors
     * l'ancien comportement : {@link #playAnimation} se fige sur la dernière frame.
     */
    @Getter
    private final Map<String, BBAnimation.LoopMode> animationLoopModes = new HashMap<>();

    /**
     * Tags de parts masquées par défaut que chaque animation doit faire apparaître
     * (bras "BAM", jambes de barrage...). cf. BBModelBaker.revealedPartTags.
     */
    @Getter
    private final Map<String, Set<String>> animationRevealedParts = new HashMap<>();

    @Getter
    private final Map<String, SoundAnimation> sounds = new HashMap<>();

    // Track active animation tasks by instanceId
    private static final Map<String, MyScheduledTask> activeAnimationTasks = new HashMap<>();

    /**
     * Durée d'interpolation appliquée à chaque frame. Le bake produit une frame
     * par tick, donc 1 tick d'interpolation suffit à lisser le mouvement côté
     * client sans décaler la pose.
     */
    private static final int FRAME_INTERPOLATION_TICKS = 1;

    /**
     * Transformation "parquée" des parts invisibles : une échelle nulle est le
     * seul moyen de masquer une display entity sans la détruire, ce qui évite un
     * cycle spawn/despawn (et les paquets qui vont avec) à chaque animation qui
     * révèle ou masque des bones.
     */
    private static final Matrix4f HIDDEN_TRANSFORM = new Matrix4f().scale(0f);

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

    // Constructeur .bbmodel brut (parsing + bake direct, sans passer par BDEngine)
    public BlockDisplayModel(NamespacedKey key, File bbmodelFile) {
        this.key = key;
        this.animated = true;

        try {
            BBModelParser.ParsedBBModel parsed = BBModelParser.parse(bbmodelFile);
            String baseName = key.getKey();

            // --- 1. Bind pose : une DisplayPart (ITEM) par bone avec géométrie ---
            List<BBModelBaker.BakedPart> bakedParts = BBModelBaker.bakeBindPose(parsed, baseName);

            BBModelAssets.ModelAssets assets = new BBModelAssets.ModelAssets();
            // Une entrée par texture du bbmodel, avec la clé "<baseName>_tex<i>"
            // (même convention que dans BBModelBaker.bakeBindPose) : même si une
            // texture n'est référencée par aucune face (cas fréquent avec des
            // calques "pasted" temporaires laissés dans Blockbench), on l'écrit
            // quand même — inoffensif, et évite un décalage d'index texture<->fichier.
            for (int i = 0; i < parsed.textures.size(); i++) {
                byte[] png = parsed.textures.get(i).pngBytes;
                if (png != null) assets.textures.put(baseName + "_tex" + i, png);
            }

            for (BBModelBaker.BakedPart part : bakedParts) {
                assets.itemModels.put(part.modelKeySuffix, part.itemModelJson);

                // PAS de préfixe "items/" ici : le composant item_model résout déjà
                // implicitement sous assets/<namespace>/items/<path>.json. Mettre
                // "items/" dans le path produirait assets/.../items/items/<...>.json
                // (inexistant) et l'item retomberait sur son rendu par défaut.
                NamespacedKey itemModelKey = new NamespacedKey(key.getNamespace(), part.modelKeySuffix);
                ItemStack displayItem = BBModelBaker.buildDisplayItem(itemModelKey);

                DisplayPart displayPart = new DisplayPart(
                        DisplayType.ITEM,
                        part.bindTransform,
                        null,
                        displayItem,
                        ItemDisplayTransform.NONE,
                        part.visibleByDefault
                );
                parts.put(part.tag, displayPart);
            }

            BBModelAssets.register(key.toString(), assets);

            // --- 2. Animations : converties au format Map<String, Map<Integer, List<AnimationFrame>>> existant ---
            Map<String, BBModelBaker.BakedAnimation> baked = BBModelBaker.bakeAnimations(parsed);
            for (Map.Entry<String, BBModelBaker.BakedAnimation> animEntry : baked.entrySet()) {
                BBModelBaker.BakedAnimation bakedAnimation = animEntry.getValue();

                Map<Integer, List<AnimationFrame>> ticks = new TreeMap<>();
                for (Map.Entry<Integer, List<BBModelBaker.BakedFrame>> tickEntry : bakedAnimation.ticks().entrySet()) {
                    List<AnimationFrame> frames = tickEntry.getValue().stream()
                            .map(f -> new AnimationFrame(f.boneTag(), DisplayType.ITEM, f.transformation(),
                                    FRAME_INTERPOLATION_TICKS, null, null))
                            .collect(Collectors.toList());
                    ticks.put(tickEntry.getKey(), frames);
                }
                animations.put(animEntry.getKey(), ticks);
                animationLoopModes.put(animEntry.getKey(), bakedAnimation.loopMode());
                animationRevealedParts.put(animEntry.getKey(), bakedAnimation.revealedPartTags());
            }

            long hiddenParts = bakedParts.stream().filter(part -> !part.visibleByDefault).count();
            KrimsonPlugin.getInstance().getLogger().info(
                    "Modèle bbmodel '" + key + "' chargé : " + bakedParts.size() + " bones ("
                            + hiddenParts + " masqué(s) par défaut), "
                            + animations.size() + " animation(s).");
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().severe("Erreur chargement bbmodel " + key + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

// ============================================================================
// NOTE sur AnimationFrame.duration :
// Le player avance tick par tick et applique interpolation_duration =
// frame.duration() à chaque tick. Le bake produit une frame par tick, donc
// FRAME_INTERPOLATION_TICKS = 1 donne une interpolation fluide sans décalage.
// Pour économiser des paquets, sous-échantillonner le bake (1 frame / N ticks)
// ET passer la même valeur N ici : les deux doivent rester cohérents, sinon la
// pose interpolée est en retard ou en avance sur la timeline.
// ============================================================================

    // --- PARSING JSON ---

    private void parseJson(JsonObject root) {
        try {
            // Si le JSON contient un wrapper "content", on descend d'un niveau
            JsonObject base = root.has("content") ? root.getAsJsonObject("content") : root;

            // 1) Initialiser les DisplayParts depuis content.passengers
            parsePassengers(base);

            // 2) Parser les animations
            if (!base.has("datapack")) {
                KrimsonPlugin.getInstance().getLogger().warning("Clé 'datapack' manquante pour le modèle " + key);
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

            parseSoundKeyframes(datapack);
            parseSoundMetadata(base);
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().severe("Erreur parsing JSON pour " + key + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseSoundKeyframes(JsonObject datapack) {
        if (!datapack.has("sound_keyframes")) return;

        JsonObject soundKeyframes = datapack.getAsJsonObject("sound_keyframes");

        // Parse all sound animation children (default, etc.)
        for (Map.Entry<String, JsonElement> soundEntry : soundKeyframes.entrySet()) {
            String animName = soundEntry.getKey();
            JsonObject soundFrames = soundEntry.getValue().getAsJsonObject();

            Map<Integer, SoundFrame> frames = new TreeMap<>();

            for (Map.Entry<String, JsonElement> frameEntry : soundFrames.entrySet()) {
                int tick = Integer.parseInt(frameEntry.getKey());
                String soundCommand = frameEntry.getValue().getAsString();
                frames.put(tick, new SoundFrame(tick, soundCommand));
            }

            // We'll set duration and step ticks later from meta
            sounds.put(animName, new SoundAnimation(animName, frames, 0, 0));
        }
    }

    private void parseSoundMetadata(JsonObject root) {
        if (!root.has("meta")) return;

        JsonObject meta = root.getAsJsonObject("meta");
        if (!meta.has("sounds")) return;

        JsonObject soundsMeta = meta.getAsJsonObject("sounds");

        // Parse sound metadata for each sound animation
        for (Map.Entry<String, JsonElement> soundEntry : soundsMeta.entrySet()) {
            String soundName = soundEntry.getKey();
            JsonObject soundData = soundEntry.getValue().getAsJsonObject();

            if (sounds.containsKey(soundName)) {
                SoundAnimation existing = sounds.get(soundName);
                int durationTicks = soundData.has("durationTicks") ? soundData.get("durationTicks").getAsInt() : 0;
                int stepTicks = soundData.has("stepTicks") ? soundData.get("stepTicks").getAsInt() : 0;

                sounds.put(soundName, new SoundAnimation(
                    existing.name(),
                    existing.soundFrames(),
                    durationTicks,
                    stepTicks
                ));
            }
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
            // Les parts masquées par défaut sont spawnées quand même, mais à
            // échelle nulle : elles doivent exister pour qu'une animation puisse
            // les révéler sans spawn/despawn en cours de route.
            Matrix4f initialTransform = part.visibleByDefault() ? part.transformation() : HIDDEN_TRANSFORM;

            Display display = null;
            if (part.type() == DisplayType.BLOCK) {
                display = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, d -> {
                    d.setBlock(part.blockData() != null ? part.blockData() : Bukkit.createBlockData(Material.STONE));
                    applyDisplayDefaults(d);
                    d.setTransformationMatrix(new Matrix4f(initialTransform));
                });
            } else if (part.type() == DisplayType.ITEM) {
                display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
                    d.setItemStack(part.itemStack() != null ? part.itemStack() : new ItemStack(Material.AIR));
                    d.setItemDisplayTransform(part.itemTransform() != null ? part.itemTransform() : ItemDisplayTransform.NONE);
                    applyDisplayDefaults(d);
                    d.setTransformationMatrix(new Matrix4f(initialTransform));
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

    /**
     * Propriétés d'affichage communes à toutes les parts (Paper 1.21.4).
     * <p>
     * {@code Billboard.FIXED} est explicite : toute autre valeur ferait pivoter
     * les parts vers la caméra de chaque joueur, ce qui détruit un rig dont
     * l'orientation est portée par la matrice. L'interpolation est armée dès le
     * spawn pour que la première frame d'animation soit déjà lissée.
     */
    private static void applyDisplayDefaults(Display display) {
        display.setPersistent(false);
        display.setBillboard(Display.Billboard.FIXED);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(FRAME_INTERPOLATION_TICKS);
        display.setViewRange(1.0f);
    }

    // --- ANIMATION ENGINE ---

    public Set<String> getAvailableAnimations() {
        return animations.keySet();
    }

    /**
     * Joue une animation en respectant le mode de bouclage déclaré par le modèle
     * ({@code loop} / {@code hold} / {@code once}).
     */
    public void playAnimation(World world, String instanceId, String animationName) {
        startAnimation(world, instanceId, animationName, null);
    }

    /** Joue une animation en forçant le bouclage, quel que soit son mode déclaré. */
    public void playAnimationLoop(World world, String instanceId, String animationName) {
        startAnimation(world, instanceId, animationName, BBAnimation.LoopMode.LOOP);
    }

    /**
     * Coeur du player d'animation, commun aux deux méthodes publiques (elles
     * étaient auparavant dupliquées à l'identique à un {@code if} près).
     *
     * @param forcedLoopMode mode imposé par l'appelant, ou {@code null} pour
     *                       utiliser celui du modèle
     */
    private void startAnimation(World world, String instanceId, String animationName,
                                BBAnimation.LoopMode forcedLoopMode) {
        if (!animated || animations.isEmpty()) return;

        Map<Integer, List<AnimationFrame>> keyframes = animations.get(animationName);
        if (keyframes == null) {
            KrimsonPlugin.getInstance().getLogger().warning("Animation '" + animationName
                    + "' not found for model " + key + ". Available animations: "
                    + String.join(", ", animations.keySet()));
            return;
        }
        if (keyframes.isEmpty()) return;

        Map<String, Display> entityMap = collectInstanceDisplays(world, instanceId);
        if (entityMap.isEmpty()) return;

        // Les modèles legacy (BDEngine / commande vanilla) n'ont pas de mode
        // déclaré : on garde leur comportement historique, à savoir se figer sur
        // la dernière frame.
        BBAnimation.LoopMode loopMode = forcedLoopMode != null
                ? forcedLoopMode
                : animationLoopModes.getOrDefault(animationName, BBAnimation.LoopMode.HOLD);

        // Parts effectivement affichées pendant cette animation : celles visibles
        // en bind pose, plus celles que cette animation révèle explicitement.
        Set<String> revealed = animationRevealedParts.getOrDefault(animationName, Collections.emptySet());
        Set<String> activeTags = new HashSet<>();
        parts.forEach((tag, part) -> {
            if (part.visibleByDefault() || revealed.contains(tag)) activeTags.add(tag);
        });

        // Toute part non active est parquée immédiatement (échelle nulle, sans
        // interpolation) : c'est ce qui masque à nouveau les bras "BAM" quand on
        // enchaîne sur une animation qui ne les utilise pas.
        entityMap.forEach((tag, display) -> {
            if (!activeTags.contains(tag)) applyTransform(display, HIDDEN_TRANSFORM, 0);
        });

        cancelActiveAnimation(instanceId);

        AnimationRunner runner = new AnimationRunner(instanceId, animationName, keyframes,
                loopMode, entityMap, activeTags);
        runner.task = KrimsonPlugin.getScheduler().runTaskTimer(runner, 0L, 1L);
        activeAnimationTasks.put(instanceId, runner.task);
    }

    /**
     * Avance une animation d'un tick par exécution.
     * <p>
     * Implémenté en classe nommée et non en lambda parce que le runnable doit
     * pouvoir annuler sa propre tâche, ce qui demande une référence à celle-ci.
     */
    private final class AnimationRunner implements Runnable {
        private final String instanceId;
        private final String animationName;
        private final Map<Integer, List<AnimationFrame>> keyframes;
        private final BBAnimation.LoopMode loopMode;
        private final Map<String, Display> entityMap;
        private final Set<String> activeTags;
        private final int maxTick;

        private MyScheduledTask task;
        private int currentTick;

        private AnimationRunner(String instanceId, String animationName,
                                Map<Integer, List<AnimationFrame>> keyframes,
                                BBAnimation.LoopMode loopMode,
                                Map<String, Display> entityMap, Set<String> activeTags) {
            this.instanceId = instanceId;
            this.animationName = animationName;
            this.keyframes = keyframes;
            this.loopMode = loopMode;
            this.entityMap = entityMap;
            this.activeTags = activeTags;
            this.maxTick = Collections.max(keyframes.keySet());
        }

        @Override
        public void run() {
            // Si l'instance a été supprimée entre-temps, la tâche doit mourir avec
            // elle plutôt que de continuer à pousser des paquets dans le vide.
            if (entityMap.values().stream().noneMatch(Display::isValid)) {
                stop();
                return;
            }

            playSoundsForTick();
            applyFramesForTick();

            currentTick++;
            if (currentTick <= maxTick) return;

            switch (loopMode) {
                case LOOP -> currentTick = 0;
                case HOLD -> stop();                       // reste sur la dernière pose
                case ONCE -> {
                    restoreBindPose();
                    stop();
                }
            }
        }

        private void playSoundsForTick() {
            SoundAnimation soundAnim = sounds.get(animationName);
            if (soundAnim == null) return;
            SoundFrame soundFrame = soundAnim.soundFrames().get(currentTick);
            if (soundFrame == null) return;
            Display reference = entityMap.values().stream()
                    .filter(Display::isValid)
                    .findFirst()
                    .orElse(null);
            if (reference != null) {
                playSound(reference.getWorld(), reference.getLocation(), soundFrame.soundCommand());
            }
        }

        private void applyFramesForTick() {
            List<AnimationFrame> frames = keyframes.get(currentTick);
            if (frames == null) return;

            for (AnimationFrame frame : frames) {
                if (!activeTags.contains(frame.partTag())) continue;

                Display display = entityMap.get(frame.partTag());
                if (display == null || !display.isValid()) continue;

                applyTransform(display, frame.transformation(), frame.duration());

                if (display instanceof BlockDisplay blockDisplay && frame.blockData() != null) {
                    blockDisplay.setBlock(frame.blockData());
                }
            }
        }

        private void restoreBindPose() {
            parts.forEach((tag, part) -> {
                Display display = entityMap.get(tag);
                if (display == null || !display.isValid()) return;
                Matrix4f target = part.visibleByDefault() ? part.transformation() : HIDDEN_TRANSFORM;
                applyTransform(display, target, FRAME_INTERPOLATION_TICKS);
            });
        }

        private void stop() {
            if (task != null) task.cancel();
            activeAnimationTasks.remove(instanceId, task);
        }
    }

    /**
     * Applique une transformation à une display entity.
     * <p>
     * L'ordre compte : {@code interpolation_delay} et {@code interpolation_duration}
     * doivent être écrits AVANT la matrice, sinon le client interpole vers la
     * nouvelle pose avec les réglages de la précédente.
     */
    private static void applyTransform(Display display, Matrix4f transformation, int durationTicks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(durationTicks);
        display.setTransformationMatrix(new Matrix4f(transformation));
    }

    /** Indexe les display entities d'une instance par tag de part. */
    private static Map<String, Display> collectInstanceDisplays(World world, String instanceId) {
        Map<String, Display> entityMap = new HashMap<>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Display display)) continue;
            String id = display.getPersistentDataContainer().get(INSTANCE_KEY, PersistentDataType.STRING);
            String partTag = display.getPersistentDataContainer().get(PART_KEY, PersistentDataType.STRING);
            if (instanceId.equals(id) && partTag != null) {
                entityMap.put(partTag, display);
            }
        }
        return entityMap;
    }

    private void playSound(World world, Location location, String soundCommand) {
        if (soundCommand == null || soundCommand.isBlank()) return;

        // Parse the sound command format: "playsound <sound> <source> <player> <x> <y> <z> <volume> <pitch>"
        String[] parts = soundCommand.split("\\s+");
        if (parts.length < 8) {
            KrimsonPlugin.getInstance().getLogger().warning("Invalid sound command format: " + soundCommand);
            return;
        }

        String soundName = parts[1];
        String source = parts[2];
        String playerSelector = parts[3];

        try {
            double x = parseCoord(parts[4]);
            double y = parseCoord(parts[5]);
            double z = parseCoord(parts[6]);
            float volume = Float.parseFloat(parts[7]);
            float pitch = parts.length > 8 ? Float.parseFloat(parts[8]) : 1.0f;

            // Calculate absolute position based on location
            double absX = location.getX() + x;
            double absY = location.getY() + y;
            double absZ = location.getZ() + z;

            // Play sound for all players (simplified - in real implementation you'd parse playerSelector)
            for (org.bukkit.entity.Player player : world.getPlayers()) {
                player.playSound(new Location(world, absX, absY, absZ), soundName, org.bukkit.SoundCategory.RECORDS, volume, pitch);
            }
        } catch (Exception e) {
            KrimsonPlugin.getInstance().getLogger().warning("Error playing sound: " + e.getMessage());
        }
    }

    // Get the first animation available and play it
    public void playAnimation(World world, String instanceId) {
        if (!animated || animations.isEmpty()) return;

        playAnimation(world, instanceId, animations.keySet().stream().findFirst().orElseThrow());
    }

    public void playAnimationLoop(World world, String instanceId) {
        if (!animated || animations.isEmpty()) return;

        // Get random animation
        String animationName = animations.keySet().stream().skip(new Random().nextInt(animations.size())).findFirst().orElseThrow();
        KrimsonPlugin.getInstance().getLogger().info("Playing animation loop for model " + key + ": " + animationName);
        playAnimationLoop(world, instanceId, animationName);
    }

    /**
     * Cancel any active animation task for the given instance
     */
    public static void cancelActiveAnimation(String instanceId) {
        MyScheduledTask task = activeAnimationTasks.get(instanceId);
        if (task != null) {
            task.cancel();
            activeAnimationTasks.remove(instanceId);
        }
    }

    /**
     * Remove all display entities associated with a model instance
     * and cancel any active animations and sounds
     */
    public static void removeModelInstance(World world, String instanceId) {
        // Cancel any active animations for this instance
        cancelActiveAnimation(instanceId);

        // Remove all entities sharing the same instance_id
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Display display) {
                String otherInstanceId = display.getPersistentDataContainer().get(INSTANCE_KEY, PersistentDataType.STRING);
                if (instanceId.equals(otherInstanceId)) {
                    display.remove();
                }
            }
        }
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
            return new DisplayPart(DisplayType.BLOCK, matrix, parseBlockData(tag.getCompound("block_state")), null, null, true);
        }
        if (id.contains("item_display") || tag.contains("item")) {
            ItemStack itemStack = parseItemStack(tag.getCompound("item"));
            ItemDisplayTransform transform = parseItemTransform(tag.getString("item_display"));
            return new DisplayPart(DisplayType.ITEM, matrix, null, itemStack, transform, true);
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

    /**
     * @param visibleByDefault false pour un bone masqué dans Blockbench : la part
     *                         est bien spawnée (son item model existe dans le pack)
     *                         mais parquée à échelle nulle jusqu'à ce qu'une
     *                         animation la révèle.
     */
    public record DisplayPart(
            DisplayType type,
            Matrix4f transformation,
            BlockData blockData,
            ItemStack itemStack,
            ItemDisplayTransform itemTransform,
            boolean visibleByDefault
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
            return new DisplayPart(type, transformation, blockData, itemStack, ItemDisplayTransform.NONE, true);
        }
    }

    // Sound data structures
    public record SoundFrame(
            int tick,
            String soundCommand
    ) {}

    public record SoundAnimation(
            String name,
            Map<Integer, SoundFrame> soundFrames,
            int durationTicks,
            int stepTicks
    ) {}
}