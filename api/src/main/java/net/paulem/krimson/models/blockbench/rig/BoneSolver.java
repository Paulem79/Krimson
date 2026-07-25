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

        float rotX = bone.rotation[0] + pose[AnimationPlayer.ROT];
        float rotY = bone.rotation[1] + pose[AnimationPlayer.ROT + 1];
        float rotZ = bone.rotation[2] + pose[AnimationPlayer.ROT + 2];

        rotY += model.parent.getRotYAdderFunction().apply(bone);

        // Debug logging for specific bones like extra_details
        //debugBoneTransformation(bone.name, bone.origin, bone.rotation, new float[] {rotX, rotY, rotZ});

        // Determine rotation method based on bone type
        boolean useDirectRotation = shouldUseDirectRotation(bone.name);
        if (useDirectRotation) {
            rotateZyx(out, rotX, rotY, rotZ);
        } else {
            rotateZyxWithConversion(out, rotX, rotY, rotZ);
        }

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

    /**
     * Determines the rotation behavior for a bone based on its name and type.
     *
     * Blockbench and Minecraft use different coordinate systems:
     * - Blockbench: Right-handed, Y-up, Z-forward
     * - Minecraft: Right-handed, Y-up, Z-backward (faces -Z)
     *
     * This requires sign inversion for X and Y rotations when converting between systems.
     * However, arms and legs are typically animated in a way that they should NOT have
     * this conversion applied, as they were already authored with the correct orientation.
     *
     * @param boneName the name of the bone to check
     * @return true if this bone should use direct rotation (no coordinate system conversion),
     *         false if it should use converted rotation (with sign inversion for X and Y)
     */
    public static boolean shouldUseDirectRotation(String boneName) {
        String lowerName = boneName.toLowerCase();

        // Arms and legs should use direct rotation (no coordinate system conversion)
        boolean isArmOrLeg = lowerName.contains("arm") || lowerName.contains("leg");

        // Special cases: some bones may need direct rotation even if not arms/legs
        // For example, "extra_details" might be positioned incorrectly with conversion
        boolean isSpecialCase = lowerName.contains("extra_details") ||
                               lowerName.contains("detail") ||
                               lowerName.contains("accessory");

        // Head and body parts typically need coordinate system conversion
        boolean isHeadOrBody = lowerName.contains("head") ||
                              lowerName.contains("body") ||
                              lowerName.contains("torso") ||
                              lowerName.contains("chest");

        // Default behavior: use coordinate system conversion for most bones
        // except arms, legs, and special cases
        return isArmOrLeg || isSpecialCase || !isHeadOrBody;
    }

    /** Applies Z, then Y, then X, in degrees. */
    public static void rotateZyx(Matrix4f matrix, float degX, float degY, float degZ) {
        if (degZ != 0.0F) {
            matrix.rotateZ((float) Math.toRadians(degZ));
        }
        if (degY != 0.0F) {
            matrix.rotateY((float) Math.toRadians(degY));
        }
        if (degX != 0.0F) {
            matrix.rotateX((float) Math.toRadians(degX));
        }
    }

    /** Applies Z, then Y, then X, in degrees, with coordinate system conversion. */
    public static void rotateZyxWithConversion(Matrix4f matrix, float degX, float degY, float degZ) {
        if (degZ != 0.0F) {
            matrix.rotateZ((float) Math.toRadians(degZ));
        }
        if (degY != 0.0F) {
            matrix.rotateY((float) Math.toRadians(-degY));
        }
        if (degX != 0.0F) {
            matrix.rotateX((float) Math.toRadians(-degX));
        }
    }

    /**
     * Debug method to log bone transformation details.
     */
    public void debugBoneTransformation(String boneName, float[] origin, float[] rotation, float[] poseRotation) {
        if (boneName.equalsIgnoreCase("extra_details") || boneName.toLowerCase().contains("detail")) {
            System.out.println("[BoneSolver] Debug for bone: " + boneName);
            System.out.println("  Origin: [" + origin[0] + ", " + origin[1] + ", " + origin[2] + "]");
            System.out.println("  Base Rotation: [" + rotation[0] + ", " + rotation[1] + ", " + rotation[2] + "]");
            System.out.println("  Pose Rotation: [" + poseRotation[0] + ", " + poseRotation[1] + ", " + poseRotation[2] + "]");
            System.out.println("  Using direct rotation: " + shouldUseDirectRotation(boneName));
        }
    }
}
