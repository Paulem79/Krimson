package net.paulem.krimson.models.blockbench.old;

import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un "element" brut du fichier .bbmodel (un cuboïde). Les coordonnées sont dans
 * l'espace natif Blockbench/Minecraft : 16 unités = 1 bloc, comme dans un model json vanilla.
 * <p>
 * Limite connue du format vanilla (donc de ce baker) : un élément ne peut avoir
 * qu'UNE SEULE rotation, sur UN SEUL axe, avec un angle parmi
 * {-45, -22.5, 0, 22.5, 45}. Blockbench autorise une rotation libre sur les 3 axes ;
 * si ton modèle utilise ça sur un élément individuel (pas sur un groupe/bone), la
 * conversion perdra en fidélité (voir {@link BBModelBaker#toVanillaAxisAngle}).
 */
public class BBElement {
    public String uuid;
    public String name;
    public final Vector3f from = new Vector3f();
    public final Vector3f to = new Vector3f();
    /** Pivot de rotation PROPRE à l'élément (rare, distinct du pivot du bone parent). */
    public final Vector3f rotationOrigin = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public float inflate = 0f;

    /** face name ("north","east","south","west","up","down") -> data */
    public final Map<String, Face> faces = new LinkedHashMap<>();

    public static class Face {
        /** [u1, v1, u2, v2] en pixels (espace texture brute, PAS encore converti en 0-16). */
        public float[] uvPixels;
        /** index dans la liste des textures du modèle, -1 si pas de texture / face masquée. */
        public int textureIndex = -1;
    }
}