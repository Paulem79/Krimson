package net.paulem.krimson.models.blockbench.rig;

import org.bukkit.NamespacedKey;

/**
 * One rigid piece of the model: the cubes that share a bone and a cube-level rotation,
 * baked into a single item model by {@code tools/generate_pack.py}.
 *
 * <p>Each part is drawn by exactly one {@code ItemDisplay}.
 */
public final class RigPart {
    /** Stable id, matching the item model file name in the pack. */
    public final String id;
    /** Bone whose animated transform drives this part. */
    public final String bone;
    /** Item model key written into the item's {@code minecraft:item_model} component. */
    public final NamespacedKey itemModel;
    /**
     * Centre of the part's baked geometry, in the bone's own coordinate space and in
     * Blockbench units. The generator shifted the geometry so this point sits at model
     * (8,8,8), which is where an item display's origin is.
     */
    public final float[] center;
    /** Cube-level rotation in degrees, applied about {@link #pivot}. */
    public final float[] rotation;
    public final float[] pivot;
    /** Whether the part is visible in the model's default pose. */
    public final boolean visibleByDefault;

    public RigPart(String id, String bone, NamespacedKey itemModel, float[] center,
                   float[] rotation, float[] pivot, boolean visibleByDefault) {
        this.id = id;
        this.bone = bone;
        this.itemModel = itemModel;
        this.center = center;
        this.rotation = rotation;
        this.pivot = pivot;
        this.visibleByDefault = visibleByDefault;
    }

    public boolean hasRotation() {
        return rotation[0] != 0.0F || rotation[1] != 0.0F || rotation[2] != 0.0F;
    }
}
