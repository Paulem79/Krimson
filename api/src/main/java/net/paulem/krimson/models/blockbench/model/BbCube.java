package net.paulem.krimson.models.blockbench.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A raw Blockbench "element" (cuboid), kept around so the model can be baked into a
 * resource pack at runtime instead of requiring a pre-built pack.
 *
 * <p>Coordinates are in Blockbench units (16 = 1 block), in the model's absolute space
 * — the same space {@link BbBone#origin} lives in. Unlike {@link BbBone}, a cube's own
 * rotation is a STATIC pose baked once into the resource pack: it is never animated, so
 * cubes are grouped by their (bone, rotation, pivot) triple into rigid "parts" that share
 * a single {@code ItemDisplay} (see {@code BbModelBaker}).
 */
public final class BbCube {
    public final float[] from = new float[3];
    public final float[] to = new float[3];
    /** Rotation pivot for this cube, only meaningful when {@link #rotation} is non-zero. */
    public final float[] origin = new float[3];
    public final float[] rotation = new float[3];
    public float inflate = 0.0F;
    public boolean visible = true;

    /** Face name ("north", "east", "south", "west", "up", "down") -> face data. */
    public final Map<String, Face> faces = new LinkedHashMap<>();

    public static final class Face {
        /** [u1, v1, u2, v2] in raw texture pixels, not yet converted to the 0-16 model space. */
        public float[] uvPixels;
        /** Index into the owning {@link BbModel#textures} list, or -1 if unset/hidden. */
        public int textureIndex = -1;
        /** Face rotation in degrees (0, 90, 180, 270). */
        public int rotation = 0;
    }
}
