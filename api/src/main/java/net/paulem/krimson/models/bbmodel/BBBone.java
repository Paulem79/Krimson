package net.paulem.krimson.models.bbmodel;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Un "bone" = un groupe de l'outliner Blockbench. Chaque bone devient UNE
 * entité item_display en jeu (cf. réponse "1 item_display par bone").
 * <p>
 * IMPORTANT sur les coordonnées bbmodel : le champ "origin" d'un groupe est
 * déjà en coordonnées ABSOLUES du modèle (pas relatif au parent). C'est pour
 * ça que {@link BBModelBaker} n'a pas besoin d'accumuler les pivots pour la
 * bind pose, seulement pour l'accumulation des ROTATIONS pendant l'animation
 * (forward kinematics), puisque les displays ne sont pas montés en Passengers
 * mais spawnés indépendamment et re-transformés à chaque tick.
 */
public class BBBone {
    public final String uuid;
    public final String name;
    /** Pivot absolu, en unités Blockbench (16 = 1 bloc). */
    public final Vector3f pivot = new Vector3f();
    /** Rotation de bind pose, en degrés. Quasi toujours (0,0,0) dans un modèle bien fait. */
    public final Vector3f bindRotation = new Vector3f();

    public BBBone parent;
    public final List<BBBone> children = new ArrayList<>();
    public final List<BBElement> ownElements = new ArrayList<>();

    public BBBone(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /** Tag utilisé comme clé de part (PART_KEY) dans BlockDisplayModel — doit rester stable. */
    public String tag() {
        return "bone_" + name.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }

    public boolean hasGeometry() {
        return !ownElements.isEmpty();
    }

    public void forEachDescendant(java.util.function.Consumer<BBBone> consumer) {
        consumer.accept(this);
        for (BBBone child : children) child.forEachDescendant(consumer);
    }
}