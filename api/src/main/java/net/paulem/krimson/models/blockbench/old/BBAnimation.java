package net.paulem.krimson.models.blockbench.old;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Une animation bbmodel ("animations[]"), avec ses animators par bone.
 * <p>
 * Les canaux sont triés et mis en cache une seule fois via
 * {@link Animator#finishLoading()} : l'ancien {@code channel(String)} refaisait
 * une {@code ArrayList} + un {@code sort} à CHAQUE appel, donc pour chaque bone,
 * chaque canal et chaque tick baké — soit des dizaines de milliers d'allocations
 * inutiles au chargement d'un modèle comme "the_world" (42 bones x 3 canaux x
 * ~25 ticks x 23 animations).
 * <p>
 * Interpolations gérées : {@code linear}, {@code catmullrom} (= "smooth" dans
 * l'UI Blockbench) et {@code step}. Ce n'est pas cosmétique : sur "the_world",
 * 1566 keyframes sur 1813 sont en {@code catmullrom}. Les traiter comme du
 * linéaire produisait des trajectoires visiblement anguleuses.
 */
public class BBAnimation {

    /** Mode de bouclage déclaré par Blockbench. */
    public enum LoopMode {
        /** Rejoue depuis 0 indéfiniment. */
        LOOP,
        /** Joue une fois puis revient à la bind pose. */
        ONCE,
        /** Joue une fois puis se fige sur la dernière frame. */
        HOLD;

        public static LoopMode parse(String raw) {
            if (raw == null) return ONCE;
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "loop" -> LOOP;
                case "hold" -> HOLD;
                default -> ONCE;
            };
        }
    }

    /** Type d'interpolation d'un keyframe vers le suivant. */
    public enum Interpolation {
        LINEAR, CATMULLROM, STEP;

        public static Interpolation parse(String raw) {
            if (raw == null) return LINEAR;
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "catmullrom", "smooth" -> CATMULLROM;
                case "step" -> STEP;
                default -> LINEAR;
            };
        }
    }

    /** Canal animable d'un bone. */
    public enum Channel {
        POSITION("position", 0f),
        ROTATION("rotation", 0f),
        SCALE("scale", 1f);

        public final String jsonName;
        /** Valeur au repos, utilisée en absence de keyframe (1 pour l'échelle, 0 sinon). */
        public final float restValue;

        Channel(String jsonName, float restValue) {
            this.jsonName = jsonName;
            this.restValue = restValue;
        }

        public static Channel fromJson(String name) {
            if (name == null) return null;
            for (Channel channel : values()) {
                if (channel.jsonName.equals(name)) return channel;
            }
            return null;
        }
    }

    public String name;
    /** Mode de bouclage brut du fichier ("loop" | "hold" | "once"). */
    public String loopMode = "loop";
    public double lengthSeconds;

    /** bone uuid -> animator (canaux position/rotation/scale) */
    public final Map<String, Animator> animators = new LinkedHashMap<>();

    private Set<String> movedBoneUuidsCache;

    public LoopMode loop() {
        return LoopMode.parse(loopMode);
    }

    public static class Animator {
        public String boneUuid;
        public String boneName;
        public final List<Keyframe> keyframes = new ArrayList<>();

        private final Map<Channel, List<Keyframe>> channels = new EnumMap<>(Channel.class);
        private boolean finalised;

        /**
         * Trie les keyframes par canal une fois pour toutes. Appelé par
         * {@link BBModelParser} à la fin du parsing de l'animator.
         */
        public void finishLoading() {
            channels.clear();
            for (Channel channel : Channel.values()) {
                List<Keyframe> list = new ArrayList<>();
                for (Keyframe keyframe : keyframes) {
                    if (keyframe.channel == channel) list.add(keyframe);
                }
                list.sort((a, b) -> Double.compare(a.time, b.time));
                channels.put(channel, Collections.unmodifiableList(list));
            }
            finalised = true;
        }

        /** Keyframes triées d'un canal. Jamais nulle. */
        public List<Keyframe> channel(Channel channel) {
            if (!finalised) finishLoading();
            List<Keyframe> list = channels.get(channel);
            return list != null ? list : Collections.emptyList();
        }

        /** true si au moins un keyframe s'écarte de la valeur de repos du canal. */
        public boolean moves() {
            for (Channel channel : Channel.values()) {
                for (Keyframe keyframe : channel(channel)) {
                    if (Math.abs(keyframe.x - channel.restValue) > 1.0E-5f
                            || Math.abs(keyframe.y - channel.restValue) > 1.0E-5f
                            || Math.abs(keyframe.z - channel.restValue) > 1.0E-5f) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class Keyframe {
        public double time; // secondes
        public Channel channel = Channel.ROTATION;
        public Interpolation interpolation = Interpolation.LINEAR;
        public float x, y, z;

        public float axis(int index) {
            return index == 0 ? x : index == 1 ? y : z;
        }
    }

    /**
     * Bones que cette animation DÉPLACE réellement (par opposition aux bones
     * simplement présents dans "animators" avec des keyframes tous à zéro).
     * <p>
     * C'est le signal qui pilote l'affichage des bones masqués : Blockbench ne
     * stocke aucun keyframe de visibilité, mais sur "the_world" la plupart des
     * animations embarquent des pistes inertes pour les bras "BAM" et les jambes
     * de barrage, alors que seules {@code Barrage}, {@code BarrageCharge} et
     * {@code KickBarrage} les animent vraiment. Se baser sur "animé" plutôt que
     * sur "présent dans les animators" donne donc exactement le bon résultat.
     */
    public Set<String> movedBoneUuids() {
        if (movedBoneUuidsCache == null) {
            Set<String> moved = new LinkedHashSet<>();
            animators.forEach((uuid, animator) -> {
                if (animator.moves()) moved.add(uuid);
            });
            movedBoneUuidsCache = Collections.unmodifiableSet(moved);
        }
        return movedBoneUuidsCache;
    }

    /** Nombre de ticks de la timeline (20 ticks/seconde), au moins 1. */
    public int totalTicks() {
        return Math.max(1, (int) Math.round(lengthSeconds * 20.0));
    }
}
