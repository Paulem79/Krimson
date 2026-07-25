package net.paulem.krimson.models.blockbench.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Blockbench group: a pivot, a rest rotation, and children.
 *
 * <p>Unlike the client-side equivalent, this holds no cube geometry. On Paper the
 * geometry lives in the resource pack; the server only needs the skeleton in order to
 * solve where each rigid part should be.
 */
public final class BbBone {
    public final String name;
    public final float[] origin = new float[3];
    public final float[] rotation = new float[3];
    public final List<BbBone> children = new ArrayList<>();

    public BbBone(String name) {
        this.name = name;
    }
}
