package net.paulem.krimson.mobs;

import net.paulem.krimson.properties.DataKey;
import org.bukkit.persistence.PersistentDataType;

/**
 * Persistent-data keys stamped onto the real Bukkit entity backing a custom mob.
 *
 * <p>The entity itself is always a genuine vanilla-derived entity (a real {@code Zombie},
 * {@code Cow}, {@code Ravager}, ...): we never invent a network-unknown entity type, since
 * the client would have nothing to render. What makes it "custom" is (a) the Java class
 * behind it, which replaces the AI wholesale, and (b) an invisible-vanilla-body +
 * Blockbench item-display rig riding along on top, exactly like {@code CustomBlock} does
 * for blocks. These keys are how {@link CustomMobManager} recognises its own entities
 * again after a chunk reload.
 */
public final class MobKeys {
    private MobKeys() {
        /* utility class */
    }

    /** The {@link CustomMobType} identifier, e.g. {@code "myplugin:wraith_zombie"}. */
    public static final DataKey<String, String> MOB_TYPE = new DataKey<>("mob_type", PersistentDataType.STRING);

    /** Marks the entity as Krimson-managed, so plain vanilla zombies are left alone. */
    public static final DataKey<Byte, Byte> CUSTOM_MOB = new DataKey<>("custom_mob", PersistentDataType.BYTE);

    /** Id of the boss instance, if this mob is a boss (used to find its {@code BossController}). */
    public static final DataKey<String, String> BOSS_ID = new DataKey<>("boss_id", PersistentDataType.STRING);
}
