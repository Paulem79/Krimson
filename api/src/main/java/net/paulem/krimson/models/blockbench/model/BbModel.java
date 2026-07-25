package net.paulem.krimson.models.blockbench.model;

import net.paulem.krimson.models.blockbench.anim.BbAnimation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The skeleton and animation library parsed out of a .bbmodel. */
public final class BbModel {
    public final List<BbBone> roots = new ArrayList<>();
    public final Map<String, BbBone> bones = new LinkedHashMap<>();
    public final Map<String, BbAnimation> animations = new LinkedHashMap<>();

    public BbAnimation animation(String name) {
        return animations.get(name);
    }
}
