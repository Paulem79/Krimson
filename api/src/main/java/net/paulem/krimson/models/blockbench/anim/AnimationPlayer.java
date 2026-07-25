package net.paulem.krimson.models.blockbench.anim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Playback state for one rig: which animation is running, how far into it, and a short
 * cross-fade out of the previous pose.
 *
 * <p>Pure logic — no Bukkit types — so it can be unit tested without a server.
 */
public final class AnimationPlayer {
    /** Layout of a sampled bone transform: pos xyz, rot xyz (degrees), scale xyz. */
    public static final int POS = 0;
    public static final int ROT = 3;
    public static final int SCALE = 6;
    public static final int STRIDE = 9;

    private static final float[] REST = {0, 0, 0, 0, 0, 0, 1, 1, 1};

    private BbAnimation current;
    private float elapsed;
    private float speed = 1.0F;
    private boolean paused;

    private final Map<String, float[]> fadeFrom = new HashMap<>();
    private float fadeDuration;
    private float fadeElapsed;

    public BbAnimation current() {
        return current;
    }

    public float elapsed() {
        return elapsed;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Bones the current animation actually displaces. The rig uses this to decide which
     * hidden parts to reveal, so it must reflect the animation in effect right now.
     */
    public Set<String> movedBones() {
        return current == null ? Collections.emptySet() : current.movedBones();
    }

    public void play(BbAnimation animation, float fadeSeconds) {
        captureFadeSource();
        this.current = animation;
        this.elapsed = 0.0F;
        this.fadeDuration = Math.max(0.0F, fadeSeconds);
        this.fadeElapsed = 0.0F;
    }

    public void stop(float fadeSeconds) {
        captureFadeSource();
        this.current = null;
        this.elapsed = 0.0F;
        this.fadeDuration = Math.max(0.0F, fadeSeconds);
        this.fadeElapsed = 0.0F;
    }

    private void captureFadeSource() {
        fadeFrom.clear();
        if (current == null) {
            return;
        }
        float local = current.localTime(elapsed);
        for (String bone : current.trackedBones()) {
            float[] snapshot = new float[STRIDE];
            sampleRaw(current, bone, local, snapshot);
            fadeFrom.put(bone, snapshot);
        }
    }

    /** Advances playback. Called once per rig tick with the real elapsed seconds. */
    public void update(float deltaSeconds) {
        if (paused) {
            return;
        }
        if (fadeDuration > 0.0F && fadeElapsed < fadeDuration) {
            fadeElapsed = Math.min(fadeDuration, fadeElapsed + deltaSeconds);
        }
        if (current == null) {
            return;
        }
        elapsed += deltaSeconds * speed;
        if (current.loopMode == BbAnimation.LoopMode.ONCE && current.isFinished(elapsed)) {
            stop(0.12F);
        }
    }

    /** True while the pose is still changing, i.e. while the rig needs packets. */
    public boolean isAnimating() {
        if (paused) {
            return false;
        }
        if (fadeDuration > 0.0F && fadeElapsed < fadeDuration) {
            return true;
        }
        if (current == null) {
            return false;
        }
        return current.loopMode == BbAnimation.LoopMode.LOOP || !current.isFinished(elapsed);
    }

    /** Writes the current transform for {@code boneName} into {@code out}. */
    public void sample(String boneName, float[] out) {
        float blend = fadeDuration <= 0.0F ? 1.0F
                : Math.min(1.0F, fadeElapsed / fadeDuration);

        if (current == null) {
            System.arraycopy(REST, 0, out, 0, STRIDE);
        } else {
            sampleRaw(current, boneName, current.localTime(elapsed), out);
        }
        if (blend >= 1.0F) {
            return;
        }
        float[] from = fadeFrom.getOrDefault(boneName, REST);
        // Euler-space blend: fine for the short fades used here, but a long fade
        // between poses with large opposing yaw would take the short-but-wrong path.
        for (int i = 0; i < STRIDE; i++) {
            out[i] = from[i] + (out[i] - from[i]) * blend;
        }
    }

    private void sampleRaw(BbAnimation animation, String boneName, float localTime,
                           float[] out) {
        BbAnimation.BoneTrack track = animation.tracks.get(boneName);
        if (track == null) {
            System.arraycopy(REST, 0, out, 0, STRIDE);
            return;
        }
        track.position.sample(localTime, out, POS);
        track.rotation.sample(localTime, out, ROT);
        track.scale.sample(localTime, out, SCALE);
    }
}
