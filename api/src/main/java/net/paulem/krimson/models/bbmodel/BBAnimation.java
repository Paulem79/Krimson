package net.paulem.krimson.models.bbmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une animation bbmodel ("animations[]"), avec ses animators par bone.
 * V1 : interpolation LINEAIRE uniquement (pas de bezier/catmullrom). Si un
 * keyframe bbmodel a interpolation="bezier"/"catmullrom", on le traite comme
 * "linear" — c'est une perte de fidélité assumée, documentée à l'utilisateur.
 */
public class BBAnimation {
    public String name;
    /** "loop" | "hold" | "once" */
    public String loopMode = "loop";
    public double lengthSeconds;

    /** bone uuid -> animator (canaux position/rotation/scale) */
    public final Map<String, Animator> animators = new LinkedHashMap<>();

    public static class Animator {
        public String boneUuid;
        public String boneName;
        public final List<Keyframe> keyframes = new ArrayList<>();

        public List<Keyframe> channel(String channel) {
            List<Keyframe> out = new ArrayList<>();
            for (Keyframe k : keyframes) if (k.channel.equals(channel)) out.add(k);
            out.sort((a, b) -> Double.compare(a.time, b.time));
            return out;
        }
    }

    public static class Keyframe {
        public double time; // secondes
        /** "position" | "rotation" | "scale" */
        public String channel;
        public String interpolation = "linear";
        public float x, y, z;
    }

    public int totalTicks() {
        return Math.max(1, (int) Math.round(lengthSeconds * 20.0));
    }
}