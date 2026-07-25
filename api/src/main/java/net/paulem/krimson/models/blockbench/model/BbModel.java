package net.paulem.krimson.models.blockbench.model;

import net.paulem.krimson.models.blockbench.anim.BbAnimation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The skeleton, geometry, textures and animation library parsed out of a .bbmodel. */
public final class BbModel {
    public final List<BbBone> roots = new ArrayList<>();
    public final Map<String, BbBone> bones = new LinkedHashMap<>();
    public final Map<String, BbAnimation> animations = new LinkedHashMap<>();

    /** Raw PNG textures embedded in the .bbmodel, in declaration order. */
    public final List<BbTexture> textures = new ArrayList<>();
    /** UV space the face coordinates are expressed in; defaults match Blockbench's own default. */
    public int textureWidth = 16;
    public int textureHeight = 16;

    public BbAnimation animation(String name) {
        return animations.get(name);
    }

    /** One texture embedded in the .bbmodel as a base64 data URI. */
    public static final class BbTexture {
        public final String name;
        public final byte[] pngBytes;

        public BbTexture(String name, byte[] pngBytes) {
            this.name = name;
            this.pngBytes = pngBytes;
        }
    }
}
