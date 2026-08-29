package net.paulem.krimsontest.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.mobs.CustomMobType;
import net.paulem.krimson.mobs.CustomMobs;
import net.paulem.krimson.mobs.boss.BossSettings;
import net.paulem.krimson.mobs.nms.CustomHorseEntity;
import net.paulem.krimson.mobs.nms.CustomZombieEntity;
import net.paulem.krimson.mobs.nms.CustomIronGolemEntity;
import net.paulem.krimson.mobs.nms.EntityTypeCast;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;

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
    public static final CustomMobType<CustomHorseEntity> GIRAFFE = CustomMobs.register("giraffe",
            key -> CustomMobType.builder(key, EntityTypeCast.<CustomHorseEntity>as(EntityType.HORSE),
                            CustomHorseEntity::new)
                    .model(new NamespacedKey(KrimsonPlugin.getInstance(), "the_world"))
                    .animation("idle", "idle")
                    .animation("walk", "walk")
                    .attribute(Attribute.MAX_HEALTH, 30.0)
                    .attribute(Attribute.MOVEMENT_SPEED, 0.2)
                    .ai((mob, goals, targets) -> {
                        // Real vanilla goals, nothing reinvented. Priority is the first argument,
                        // exactly like a data-driven vanilla mob's goal list would be.
                        goals.addGoal(0, new FloatGoal(mob));
                        goals.addGoal(1, new WaterAvoidingRandomStrollGoal(mob, 1.0));
                        goals.addGoal(2, new LookAtPlayerGoal(mob, net.minecraft.world.entity.player.Player.class, 8.0F));
                        goals.addGoal(3, new RandomLookAroundGoal(mob));
                        // No targetSelector goals: a giraffe never attacks or gets angry.
                    })
                    .build());

    /** Mechanically a real zombie - it still burns in daylight and converts to a drowned in water,
     *  since it extends Zombie directly - wearing a completely custom body and a beefed-up kit. */
    public static final CustomMobType<CustomZombieEntity> WRAITH_ZOMBIE = CustomMobs.register("wraith_zombie",
            key -> CustomMobType.builder(key, EntityTypeCast.<CustomZombieEntity>as(EntityType.ZOMBIE),
                            CustomZombieEntity::new)
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
                    .ai((mob, goals, targets) -> {
                        goals.addGoal(0, new FloatGoal(mob));
                        goals.addGoal(1, new MeleeAttackGoal(mob, 1.15, false));
                        goals.addGoal(2, new WaterAvoidingRandomStrollGoal(mob, 1.0));
                        goals.addGoal(3, new LookAtPlayerGoal(mob, net.minecraft.world.entity.player.Player.class, 8.0F));
                        goals.addGoal(4, new RandomLookAroundGoal(mob));

                        targets.addGoal(0, new HurtByTargetGoal(mob));
                        targets.addGoal(1, new NearestAttackableTargetGoal<>(
                                mob, net.minecraft.world.entity.player.Player.class, true));
                    })
                    .onSpawn(mob -> {
                        // Anything else you'd want on the raw NMS entity before it's added to the
                        // world - custom NBT, persistence flags, whatever - goes here.
                    })
                    .build());

    /** A boss with an iron-golem body (big hitbox, high knockback resistance) plus a bar and a phase. */
    public static final CustomMobType<CustomIronGolemEntity> CINDER_TITAN = CustomMobs.register("cinder_titan",
            key -> CustomMobType.builder(key, EntityTypeCast.<CustomIronGolemEntity>as(EntityType.IRON_GOLEM),
                            CustomIronGolemEntity::new)
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
                    .ai((mob, goals, targets) -> {
                        goals.addGoal(0, new MeleeAttackGoal(mob, 1.0, true));
                        goals.addGoal(1, new WaterAvoidingRandomStrollGoal(mob, 0.6));
                        goals.addGoal(2, new LookAtPlayerGoal(mob, net.minecraft.world.entity.player.Player.class, 10.0F));
                        goals.addGoal(3, new RandomLookAroundGoal(mob));

                        targets.addGoal(0, new NearestAttackableTargetGoal<>(
                                mob, net.minecraft.world.entity.player.Player.class, true));
                    })
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
