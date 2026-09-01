package net.paulem.krimsontest.mobs;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.mobs.CustomMobType;
import net.paulem.krimson.mobs.CustomMobs;
import net.paulem.krimson.mobs.ai.goals.FloatGoal;
import net.paulem.krimson.mobs.ai.goals.LookAtNearestPlayerGoal;
import net.paulem.krimson.mobs.ai.goals.MeleeAttackGoal;
import net.paulem.krimson.mobs.ai.goals.TargetNearestPlayerGoal;
import net.paulem.krimson.mobs.ai.goals.WanderGoal;
import net.paulem.krimson.mobs.boss.BossSettings;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Zombie;

/**
 * Three worked examples of the custom mob system: a harmless passive critter with its own
 * model, a hostile monster that's mechanically "just a zombie" wearing a different
 * skeleton, and a boss with a bar and phases.
 *
 * <p>Each references a model registered the normal way in {@code PluginModels} (a
 * {@code BlockbenchDisplayModel} pointed at a {@code .bbmodel} under
 * {@code resources/assets/<namespace>/<key>.bbmodel}) - the exact same pipeline
 * {@code THE_WORLD} already uses for the block-mounted display, just puppeted on a mob's
 * body instead of standing on its own.
 */
public final class PluginMobs {
    private PluginMobs() {
        /* utility class */
    }

    /** A tall, entirely peaceful custom animal. Body: an invisible horse (for the taller hitbox). */
    public static final CustomMobType<Horse> GIRAFFE = CustomMobs.register("giraffe",
            key -> CustomMobType.builder(key, Horse.class)
                    .model(new NamespacedKey(KrimsonPlugin.getInstance(), "the_world"))
                    .animation("idle", "idle")
                    .animation("walk", "walk")
                    .attribute(Attribute.MAX_HEALTH, 30.0)
                    .attribute(Attribute.MOVEMENT_SPEED, 0.2)
                    // Bukkit-only goals, no NMS. Priority is the first constructor argument,
                    // exactly like a data-driven vanilla mob's goal list would be.
                    .ai(
                            new FloatGoal(0),
                            new WanderGoal(1, 1.0),
                            new LookAtNearestPlayerGoal(2, 8.0)
                            // No target goal: a giraffe never attacks or gets angry.
                    )
                    .build());

    /** Mechanically a real zombie - it still burns in daylight and converts to a drowned in water,
     *  since it extends Zombie directly - wearing a completely custom body and a beefed-up kit. */
    public static final CustomMobType<Zombie> WRAITH_ZOMBIE = CustomMobs.register("wraith_zombie",
            key -> CustomMobType.builder(key, Zombie.class)
                    .model(new NamespacedKey(KrimsonPlugin.getInstance(), "the_world"))
                    .animation("idle", "idle")
                    .animation("walk", "walk")
                    .animation("attack", "attack")
                    .animation("hurt", "hurt")
                    .animation("death", "death")
                    .attackAnimSeconds(0.4F)
                    .attribute(Attribute.MAX_HEALTH, 40.0)
                    .attribute(Attribute.ATTACK_DAMAGE, 8.0)
                    .attribute(Attribute.MOVEMENT_SPEED, 0.27)
                    .attribute(Attribute.ARMOR, 4.0)
                    .ai(
                            new FloatGoal(0),
                            new MeleeAttackGoal(1, 1.15),
                            new WanderGoal(2, 1.0),
                            new LookAtNearestPlayerGoal(3, 8.0),
                            new TargetNearestPlayerGoal(0, 16.0)
                            // HurtByTargetGoal's job (retaliate on damage) is handled by
                            // CustomMobListener#onDamage for every custom mob, not per-type here.
                    )
                    .onSpawn(mob -> {
                        // Anything else you'd want on the Bukkit entity before it's added to the
                        // world - custom NBT, persistence flags, whatever - goes here.
                    })
                    .build());

    /** A boss with an iron-golem body (big hitbox, high knockback resistance) plus a bar and a phase. */
    public static final CustomMobType<IronGolem> CINDER_TITAN = CustomMobs.register("cinder_titan",
            key -> CustomMobType.builder(key, IronGolem.class)
                    .model(new NamespacedKey(KrimsonPlugin.getInstance(), "the_world"), 1.6F)
                    .animation("idle", "idle")
                    .animation("walk", "walk")
                    .animation("attack", "slam")
                    .animation("hurt", "hurt")
                    .animation("death", "collapse")
                    .attackAnimSeconds(0.6F)
                    .despawnAfterDeathSeconds(2.0F)
                    .attribute(Attribute.MAX_HEALTH, 250.0)
                    .attribute(Attribute.ATTACK_DAMAGE, 15.0)
                    .attribute(Attribute.ARMOR, 10.0)
                    .attribute(Attribute.MOVEMENT_SPEED, 0.22)
                    .attribute(Attribute.KNOCKBACK_RESISTANCE, 1.0)
                    .ai(
                            new MeleeAttackGoal(1, 1.0),
                            new WanderGoal(2, 0.6),
                            new LookAtNearestPlayerGoal(3, 10.0),
                            new TargetNearestPlayerGoal(0, 20.0)
                    )
                    .boss(BossSettings.builder("§4§lCinder Titan")
                            .color(BarColor.RED)
                            // Below 50% health: speed up and roar. Add as many phases as the fight needs.
                            .phase(0.5, controller -> {
                                controller.entity().getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.30);
                                controller.entity().getWorld().strikeLightningEffect(controller.entity().getLocation());
                            })
                            .build())
                    .build());

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering mobs: "
                + GIRAFFE.getKey() + ", " + WRAITH_ZOMBIE.getKey() + ", " + CINDER_TITAN.getKey());
    }
}
