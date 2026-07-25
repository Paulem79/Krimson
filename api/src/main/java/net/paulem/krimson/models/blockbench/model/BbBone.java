package net.paulem.krimson.models.blockbench.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Blockbench group: a pivot, a rest rotation, and children.
 *
 * <p>The server only needs the skeleton to solve where each rigid part should be at
 * runtime (see {@code BoneSolver}), which never touches {@link #cubes}. The cubes are
 * kept here purely so the model can be baked into a resource pack (textures + item
 * models) on load, without needing a pre-built pack: see {@code BbModelBaker}.
 */
public final class BbBone {
    public final String name;
    public final float[] origin = new float[3];
    public final float[] rotation = new float[3];
    public final List<BbBone> children = new ArrayList<>();
    /** Cubes attached directly to this bone (not to a nested child group). */
    public final List<BbCube> cubes = new ArrayList<>();

    public BbBone(String name) {
        this.name = name;
    }
}
