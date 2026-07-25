package net.paulem.krimson.models.blockbench.anim;

import java.util.*;

/** One Blockbench animation: per-bone position / rotation / scale tracks. */
public final class BbAnimation {
    public enum LoopMode {
        /** Restart from 0 forever. */
        LOOP,
        /** Play once, then stop and fall back to the rest pose. */
        ONCE,
        /** Play once, then freeze on the final frame. */
        HOLD;

        public static LoopMode parse(String raw) {
            if (raw == null) {
                return ONCE;
            }
            return switch (raw.toLowerCase()) {
                case "loop" -> LOOP;
                case "hold" -> HOLD;
                default -> ONCE;
            };
        }
    }

    public enum Interpolation { LINEAR, CATMULLROM, STEP;
        public static Interpolation parse(String raw) {
            if (raw == null) {
                return LINEAR;
            }
            return switch (raw.toLowerCase()) {
                case "catmullrom", "smooth" -> CATMULLROM;
                case "step" -> STEP;
                default -> LINEAR;
            };
        }
    }

    public static final class Keyframe {
        public final float time;
        public final Interpolation interpolation;
        public final float x, y, z;

        public Keyframe(float time, Interpolation interpolation, float x, float y, float z) {
            this.time = time;
            this.interpolation = interpolation;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        float get(int axis) {
            return axis == 0 ? x : axis == 1 ? y : z;
        }
    }

    /** A sorted keyframe list for one channel of one bone. */
    public static final class Channel {
        public final List<Keyframe> keyframes = new ArrayList<>();
        private final float defaultValue;

        Channel(float defaultValue) {
            this.defaultValue = defaultValue;
        }

        void sort() {
            keyframes.sort((a, b) -> Float.compare(a.time, b.time));
        }

        public boolean isEmpty() {
            return keyframes.isEmpty();
        }

        /** True if any keyframe departs from the channel's rest value. */
        boolean moves() {
            for (Keyframe k : keyframes) {
                for (int axis = 0; axis < 3; axis++) {
                    if (Math.abs(k.get(axis) - defaultValue) > 1.0E-5F) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Samples all three axes at {@code time} into {@code dst}. */
        public void sample(float time, float[] dst, int offset) {
            int n = keyframes.size();
            if (n == 0) {
                dst[offset] = dst[offset + 1] = dst[offset + 2] = defaultValue;
                return;
            }
            if (n == 1 || time <= keyframes.get(0).time) {
                store(keyframes.get(0), dst, offset);
                return;
            }
            if (time >= keyframes.get(n - 1).time) {
                store(keyframes.get(n - 1), dst, offset);
                return;
            }

            int i = 0;
            while (i < n - 1 && keyframes.get(i + 1).time <= time) {
                i++;
            }
            Keyframe k0 = keyframes.get(i);
            Keyframe k1 = keyframes.get(i + 1);
            float span = k1.time - k0.time;
            float t = span <= 1.0E-6F ? 0.0F : (time - k0.time) / span;

            if (k0.interpolation == Interpolation.STEP) {
                store(k0, dst, offset);
                return;
            }
            if (k0.interpolation == Interpolation.CATMULLROM) {
                Keyframe kPrev = i > 0 ? keyframes.get(i - 1) : k0;
                Keyframe kNext = i + 2 < n ? keyframes.get(i + 2) : k1;
                for (int axis = 0; axis < 3; axis++) {
                    dst[offset + axis] = catmullRom(kPrev.get(axis), k0.get(axis),
                            k1.get(axis), kNext.get(axis), t);
                }
                return;
            }
            for (int axis = 0; axis < 3; axis++) {
                dst[offset + axis] = k0.get(axis) + (k1.get(axis) - k0.get(axis)) * t;
            }
        }

        private static void store(Keyframe k, float[] dst, int offset) {
            dst[offset] = k.x;
            dst[offset + 1] = k.y;
            dst[offset + 2] = k.z;
        }
    }

    /** Uniform Catmull-Rom, matching Blockbench's "smooth" keyframe interpolation. */
    public static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2.0F * p1)
                + (-p0 + p2) * t
                + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
    }

    /** All three channels for a single bone. */
    public static final class BoneTrack {
        public final Channel position = new Channel(0.0F);
        public final Channel rotation = new Channel(0.0F);
        public final Channel scale = new Channel(1.0F);

        void sort() {
            position.sort();
            rotation.sort();
            scale.sort();
        }

        boolean moves() {
            return position.moves() || rotation.moves() || scale.moves();
        }
    }

    public final String name;
    /** Length in seconds, as authored in Blockbench. */
    public final float length;
    public final LoopMode loopMode;
    public final Map<String, BoneTrack> tracks = new LinkedHashMap<>();

    private Set<String> movedBonesCache;

    public BbAnimation(String name, float length, LoopMode loopMode) {
        this.name = name;
        this.length = length;
        this.loopMode = loopMode;
    }

    public BoneTrack track(String boneName) {
        return tracks.computeIfAbsent(boneName, key -> new BoneTrack());
    }

    public void finishLoading() {
        tracks.values().forEach(BoneTrack::sort);
    }

    public Set<String> trackedBones() {
        return Collections.unmodifiableSet(tracks.keySet());
    }

    /** Bones whose keyframes actually leave the rest pose. Used to gate visibility. */
    public Set<String> movedBones() {
        if (movedBonesCache == null) {
            Set<String> moved = new LinkedHashSet<>();
            tracks.forEach((bone, track) -> {
                if (track.moves()) {
                    moved.add(bone);
                }
            });
            movedBonesCache = Collections.unmodifiableSet(moved);
        }
        return movedBonesCache;
    }

    /**
     * Maps elapsed playback time onto the animation's own timeline.
     *
     * @return the local time in seconds, clamped or wrapped per {@link LoopMode}
     */
    public float localTime(float elapsedSeconds) {
        if (length <= 1.0E-6F) {
            return 0.0F;
        }
        if (loopMode == LoopMode.LOOP) {
            float t = elapsedSeconds % length;
            return t < 0.0F ? t + length : t;
        }
        return Math.min(elapsedSeconds, length);
    }

    /** True once a non-looping animation has run past its end. */
    public boolean isFinished(float elapsedSeconds) {
        return loopMode != LoopMode.LOOP && elapsedSeconds >= length;
    }
}
