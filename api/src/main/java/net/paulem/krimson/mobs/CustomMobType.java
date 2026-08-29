package net.paulem.krimson.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import net.paulem.krimson.mobs.boss.BossSettings;
import net.paulem.krimson.mobs.nms.KrimsonMob;
import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
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
 * <h2>Why it's a "true copy" of the vanilla mob system, not a re-implementation</h2>
 * The AI is not reinvented: {@link #goalConfigurator()} hands you the entity's <b>real</b>
 * {@code net.minecraft.world.entity.ai.goal.GoalSelector}s, the same ones vanilla mobs use.
 * You can add stock vanilla goals ({@code new MeleeAttackGoal(...)}, {@code new
 * WaterAvoidingRandomStrollGoal(...)}, ...), goals from any other mod/plugin, or your own
 * {@code Goal} subclasses - Krimson does not get in the way. Attributes
 * ({@link #attributeOverrides()}) are the same {@link Attribute} instances vanilla mobs are
 * tuned with. The only genuinely custom part is the body: the backing entity is turned
 * invisible and a Blockbench item-display rig (see {@code BlockbenchDisplayModel}) is
 * puppeted on top of it every tick, which is how the giraffe/boss/reskinned-zombie all get
 * their own texture and skeleton despite being, mechanically, a real zombie/cow/ravager.
 *
 * @param <T> the NMS class backing this mob - see {@link net.paulem.krimson.mobs.nms}
 *            for the three ready-made shapes (monster, animal, generic pathfinder mob)
 */
public final class CustomMobType<T extends Mob & KrimsonMob<T>> implements RegistryKey<NamespacedKey> {

    /** Hooks the real vanilla goal selectors of a freshly-created mob. This IS the AI. */
    @FunctionalInterface
    public interface GoalConfigurator<T extends Mob> {
        void configure(T mob, GoalSelector goalSelector, GoalSelector targetSelector);
    }

    /** Constructs the backing NMS entity. Almost always a constructor reference. */
    @FunctionalInterface
    public interface MobFactory<T extends Mob> {
        T create(EntityType<T> type, Level level);
    }

    private final NamespacedKey key;
    private final EntityType<T> baseEntityType;
    private final MobFactory<T> factory;
    private final GoalConfigurator<T> goalConfigurator;

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
        this.goalConfigurator = builder.goalConfigurator;
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

    public GoalConfigurator<T> goalConfigurator() {
        return goalConfigurator;
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
        private GoalConfigurator<T> goalConfigurator = (mob, goals, targets) -> {
            /* no AI by default: an unconfigured mob just stands there */
        };

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
         * The hook where you write the mob's brain, using the same {@code GoalSelector}
         * and {@code Goal} classes vanilla itself uses.
         */
        public Builder<T> ai(GoalConfigurator<T> configurator) {
            this.goalConfigurator = configurator;
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
