package net.paulem.krimson.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.paulem.krimson.mobs.ai.KrimsonGoal;
import net.paulem.krimson.mobs.boss.BossSettings;
import net.paulem.krimson.mobs.nms.KrimsonMob;
import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The full definition of one custom mob: what vanilla entity it is really made of, what
 * it looks like, what it plays when it does things, how strong it is, and how it thinks.
 *
 * <p>This is the mob equivalent of {@code CustomBlock}/{@code CustomItem}: a template you
 * register once (see {@link CustomMobs#register}) and then spawn many times via
 * {@link CustomMobs#spawn}.
 *
 * <h2>Body vs. brain</h2>
 * The body is real: the backing entity is a real vanilla NMS mob (kept invisible), which is
 * what gives it working physics, gravity, collisions and pathfinding for free - a Blockbench
 * item-display rig (see {@code BlockbenchDisplayModel}) is puppeted on top of it every tick,
 * which is how the giraffe/boss/reskinned-zombie all get their own texture and skeleton
 * despite being, mechanically, a real zombie/cow/ravager. Attributes
 * ({@link #attributeOverrides()}) are the same {@link Attribute} instances vanilla mobs are
 * tuned with.
 *
 * <p>The brain is not: AI ({@link #aiGoals()}) is a list of {@link KrimsonGoal}s, written
 * entirely against the public {@code org.bukkit.entity.Mob} API - no
 * {@code net.minecraft.world.entity.ai.goal.Goal}, no {@code GoalSelector}. Vanilla's own
 * goal selector is wiped on spawn and never touched again.
 *
 * @param <T> the NMS class backing this mob's body - see {@link net.paulem.krimson.mobs.nms}
 *            for the three ready-made shapes (monster, animal, generic pathfinder mob)
 */
public final class CustomMobType<T extends Mob & KrimsonMob<T>> implements RegistryKey<NamespacedKey> {

    /** Constructs the backing NMS entity. Almost always a constructor reference. */
    @FunctionalInterface
    public interface MobFactory<T extends Mob> {
        T create(EntityType<T> type, Level level);
    }

    private final NamespacedKey key;
    private final EntityType<T> baseEntityType;
    private final MobFactory<T> factory;
    private final List<KrimsonGoal> aiGoals;

    private final NamespacedKey modelKey;
    private final Map<String, String> animations;
    private final float modelScale;
    private final Vector3f rigOffset;

    private final Map<Attribute, Double> attributeOverrides;

    private final float hurtAnimSeconds;
    private final float attackAnimSeconds;
    private final float despawnAfterDeathSeconds;

    @Nullable
    private final BossSettings bossSettings;

    @Nullable
    private final Consumer<T> onSpawn;

    private CustomMobType(Builder<T> builder) {
        this.key = builder.key;
        this.baseEntityType = builder.baseEntityType;
        this.factory = builder.factory;
        this.aiGoals = List.copyOf(builder.aiGoals);
        this.modelKey = builder.modelKey;
        this.animations = Map.copyOf(builder.animations);
        this.modelScale = builder.modelScale;
        this.rigOffset = builder.rigOffset;
        this.attributeOverrides = Map.copyOf(builder.attributeOverrides);
        this.hurtAnimSeconds = builder.hurtAnimSeconds;
        this.attackAnimSeconds = builder.attackAnimSeconds;
        this.despawnAfterDeathSeconds = builder.despawnAfterDeathSeconds;
        this.bossSettings = builder.bossSettings;
        this.onSpawn = builder.onSpawn;
    }

    public static <T extends Mob & KrimsonMob<T>> Builder<T> builder(NamespacedKey key, EntityType<T> baseEntityType,
                                                                       MobFactory<T> factory) {
        return new Builder<>(key, baseEntityType, factory);
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    public EntityType<T> baseEntityType() {
        return baseEntityType;
    }

    public MobFactory<T> factory() {
        return factory;
    }

    public List<KrimsonGoal> aiGoals() {
        return aiGoals;
    }

    public NamespacedKey modelKey() {
        return modelKey;
    }

    /** State name ({@code "walk"}, {@code "attack"}, ...) -> animation name in the .bbmodel. */
    public Map<String, String> animations() {
        return animations;
    }

    @Nullable
    public String animationFor(String state) {
        return animations.get(state);
    }

    public float modelScale() {
        return modelScale;
    }

    public Vector3f rigOffset() {
        return rigOffset;
    }

    public Map<Attribute, Double> attributeOverrides() {
        return attributeOverrides;
    }

    public float hurtAnimSeconds() {
        return hurtAnimSeconds;
    }

    public float attackAnimSeconds() {
        return attackAnimSeconds;
    }

    public float despawnAfterDeathSeconds() {
        return despawnAfterDeathSeconds;
    }

    public boolean isBoss() {
        return bossSettings != null;
    }

    @Nullable
    public BossSettings bossSettings() {
        return bossSettings;
    }

    @Nullable
    public Consumer<T> onSpawn() {
        return onSpawn;
    }

    public static final class Builder<T extends Mob & KrimsonMob<T>> {
        private final NamespacedKey key;
        private final EntityType<T> baseEntityType;
        private final MobFactory<T> factory;
        private final List<KrimsonGoal> aiGoals = new ArrayList<>();

        private NamespacedKey modelKey;
        private final Map<String, String> animations = new LinkedHashMap<>();
        private float modelScale = 1.0F;
        private Vector3f rigOffset = new Vector3f(0, 0, 0);

        private final Map<Attribute, Double> attributeOverrides = new LinkedHashMap<>();

        private float hurtAnimSeconds = 0.35F;
        private float attackAnimSeconds = 0.5F;
        private float despawnAfterDeathSeconds = 1.2F;

        @Nullable
        private BossSettings bossSettings;

        @Nullable
        private Consumer<T> onSpawn;

        private Builder(NamespacedKey key, EntityType<T> baseEntityType, MobFactory<T> factory) {
            this.key = key;
            this.baseEntityType = baseEntityType;
            this.factory = factory;
        }

        /**
         * Adds one or more {@link KrimsonGoal}s to this mob's brain - see
         * {@code net.paulem.krimson.mobs.ai.goals} for the ready-made ones
         * ({@code WanderGoal}, {@code MeleeAttackGoal}, {@code TargetNearestPlayerGoal}, ...).
         */
        public Builder<T> ai(KrimsonGoal... goals) {
            this.aiGoals.addAll(List.of(goals));
            return this;
        }

        /** Which registered {@code BlockbenchDisplayModel} to puppet on this mob's body. */
        public Builder<T> model(NamespacedKey modelKey) {
            this.modelKey = modelKey;
            return this;
        }

        public Builder<T> model(NamespacedKey modelKey, float scale) {
            this.modelKey = modelKey;
            this.modelScale = scale;
            return this;
        }

        public Builder<T> rigOffset(float x, float y, float z) {
            this.rigOffset = new Vector3f(x, y, z);
            return this;
        }

        /** Maps a state ({@code "idle"}, {@code "walk"}, {@code "attack"}, {@code "hurt"}, {@code "death"}) to a bbmodel animation name. */
        public Builder<T> animation(String state, String animationName) {
            this.animations.put(state, animationName);
            return this;
        }

        public Builder<T> attribute(Attribute attribute, double value) {
            this.attributeOverrides.put(attribute, value);
            return this;
        }

        public Builder<T> hurtAnimSeconds(float seconds) {
            this.hurtAnimSeconds = seconds;
            return this;
        }

        public Builder<T> attackAnimSeconds(float seconds) {
            this.attackAnimSeconds = seconds;
            return this;
        }

        public Builder<T> despawnAfterDeathSeconds(float seconds) {
            this.despawnAfterDeathSeconds = seconds;
            return this;
        }

        public Builder<T> boss(BossSettings settings) {
            this.bossSettings = settings;
            return this;
        }

        /** Runs right after the entity is constructed, before it's added to the world. */
        public Builder<T> onSpawn(Consumer<T> onSpawn) {
            this.onSpawn = onSpawn;
            return this;
        }

        public CustomMobType<T> build() {
            if (modelKey == null) {
                throw new IllegalStateException("CustomMobType " + key + " has no model() set");
            }
            return new CustomMobType<>(this);
        }
    }
}
