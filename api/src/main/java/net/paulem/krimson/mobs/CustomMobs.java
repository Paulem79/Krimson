package net.paulem.krimson.mobs;

import net.minecraft.world.entity.Mob;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.mobs.nms.KrimsonMob;
import net.paulem.krimson.registry.NewFrozenRegistry;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;

/**
 * Registry of every {@link CustomMobType}, and the entry point for spawning them.
 *
 * <pre>{@code
 * public static final CustomMobType<CustomHorseEntity> GIRAFFE = CustomMobs.register("giraffe",
 *     key -> CustomMobType.builder(key, EntityTypeCast.<CustomHorseEntity>as(EntityType.HORSE), CustomHorseEntity::new)
 *         .model(new NamespacedKey("myplugin", "giraffe"))
 *         .animation("idle", "idle").animation("walk", "walk")
 *         .attribute(Attribute.MAX_HEALTH, 20.0).attribute(Attribute.MOVEMENT_SPEED, 0.28)
 *         .ai((mob, goals, targets) -> {
 *             goals.addGoal(1, new FloatGoal(mob));
 *             goals.addGoal(2, new WaterAvoidingRandomStrollGoal(mob, 1.0));
 *             goals.addGoal(3, new RandomLookAroundGoal(mob));
 *         })
 *         .build());
 *
 * CustomMobs.spawn(GIRAFFE, player.getLocation());
 * }</pre>
 */
public final class CustomMobs {
    private CustomMobs() {
        /* utility class */
    }

    public static final NewFrozenRegistry<CustomMobType<?>, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    /** Lazily created on first use; started/stopped by {@code KrimsonAPI}. */
    static CustomMobManager MANAGER;

    public static void init() {
        if (MANAGER == null) {
            MANAGER = new CustomMobManager(KrimsonPlugin.getInstance());
            MANAGER.start();
        }
    }

    public static void shutdown() {
        if (MANAGER != null) {
            MANAGER.stop();
            MANAGER = null;
        }
    }

    public static <T extends Mob & KrimsonMob<T>> CustomMobType<T> register(String key,
            java.util.function.Function<NamespacedKey, CustomMobType<T>> factory) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);
        CustomMobType<T> type = factory.apply(identifier);
        REGISTRY.register(type);
        KrimsonPlugin.getInstance().getLogger().info("Registered custom mob: " + identifier.getKey());
        return type;
    }

    public static <T extends Mob & KrimsonMob<T>> CustomMobInstance spawn(CustomMobType<T> type, Location location) {
        return MANAGER.spawn(type, location);
    }

    public static CustomMobManager manager() {
        return MANAGER;
    }
}
