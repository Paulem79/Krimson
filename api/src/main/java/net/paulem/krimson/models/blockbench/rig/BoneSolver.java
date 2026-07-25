package net.paulem.krimson.models.blockbench.rig;

import net.paulem.krimson.models.blockbench.anim.AnimationPlayer;
import net.paulem.krimson.models.blockbench.model.BbBone;
import net.paulem.krimson.models.blockbench.model.BbModel;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a pose into one world matrix per bone, in Blockbench units (1 = 1/16 block,
 * +Y up, feet at y = 0).
 *
 * <p>Per bone the local transform is
 * {@code T(origin + animPos) * Rz * Ry * Rx * S * T(-origin)} — the same order vanilla
 * {@code ModelPart} and Blockbench use, so poses match the Blockbench preview.
 *
 * <p>Matrices are allocated once per bone and rewritten in place, since this runs for
 * every rig on every tick.
 */
public final class BoneSolver {
    private final BbModel model;
    private final Map<String, Matrix4f> world = new HashMap<>();
    private final float[] pose = new float[AnimationPlayer.STRIDE];

    public BoneSolver(BbModel model) {
        this.model = model;
        for (String name : model.bones.keySet()) {
            world.put(name, new Matrix4f());
        }
    }

    /**
     * Solves every bone.
     *
     * @param root   transform applied above the model root — carries the rig's yaw and
     *               scale, so those flow into every part automatically
     * @param player pose source, or {@code null} for the rest pose
     */
    public void solve(Matrix4f root, AnimationPlayer player) {
        for (BbBone bone : model.roots) {
            solveBone(bone, root, player);
        }
    }

    private void solveBone(BbBone bone, Matrix4f parent, AnimationPlayer player) {
        if (player != null) {
            player.sample(bone.name, pose);
        } else {
            pose[AnimationPlayer.POS] = pose[AnimationPlayer.POS + 1] =
                    pose[AnimationPlayer.POS + 2] = 0.0F;
            pose[AnimationPlayer.ROT] = pose[AnimationPlayer.ROT + 1] =
                    pose[AnimationPlayer.ROT + 2] = 0.0F;
            pose[AnimationPlayer.SCALE] = pose[AnimationPlayer.SCALE + 1] =
                    pose[AnimationPlayer.SCALE + 2] = 1.0F;
        }

        Matrix4f out = world.get(bone.name);
        out.set(parent);
        out.translate(bone.origin[0] + pose[AnimationPlayer.POS],
                bone.origin[1] + pose[AnimationPlayer.POS + 1],
                bone.origin[2] + pose[AnimationPlayer.POS + 2]);

        float rotY = bone.rotation[1] + pose[AnimationPlayer.ROT + 1];

        rotY += model.parent.getRotYAdderFunction().apply(bone);

        rotateZyx(out,
                bone.rotation[0] + pose[AnimationPlayer.ROT],
                rotY,
                bone.rotation[2] + pose[AnimationPlayer.ROT + 2]);

        float sx = pose[AnimationPlayer.SCALE];
        float sy = pose[AnimationPlayer.SCALE + 1];
        float sz = pose[AnimationPlayer.SCALE + 2];
        if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
            out.scale(sx, sy, sz);
        }
        out.translate(-bone.origin[0], -bone.origin[1], -bone.origin[2]);

        for (BbBone child : bone.children) {
            solveBone(child, out, player);
        }
    }

    public Matrix4f matrixOf(String boneName) {
        return world.get(boneName);
    }

    /** Applies Z, then Y, then X, in degrees. */
    public static void rotateZyx(Matrix4f matrix, float degX, float degY, float degZ) {
        if (degZ != 0.0F) {
            matrix.rotateZ((float) Math.toRadians(degZ));
        }
        if (degY != 0.0F) {
            matrix.rotateY((float) Math.toRadians(-degY)); // Note l'inversion du signe (-degY)
        }
        if (degX != 0.0F) {
            matrix.rotateX((float) Math.toRadians(-degX)); // Note l'inversion du signe (-degX)
        }
    }
}
