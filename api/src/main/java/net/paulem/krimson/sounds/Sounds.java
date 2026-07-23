package net.paulem.krimson.sounds;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.registry.NewFrozenRegistry;
import org.bukkit.NamespacedKey;

import java.util.function.Function;

/**
 * Registry utility for custom sounds — mirrors the pattern of
 * {@link net.paulem.krimson.blocks.Blocks},
 * {@link net.paulem.krimson.items.Items}, and
 * {@link net.paulem.krimson.models.Models}.
 *
 * <p>Usage:
 * <pre>{@code
 * public static final CustomSound MY_SOUND = Sounds.registerSound("my_sound", CustomSound::new);
 * }</pre>
 */
public class Sounds {
    public static final NewFrozenRegistry<CustomSound, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    /**
     * Registers a custom sound with the given key.
     *
     * @param key     the sound name (e.g. "my_sound"), will be namespaced with the plugin's namespace
     * @param factory factory function receiving the NamespacedKey, returning a CustomSound
     * @param <T>     the specific CustomSound subtype
     * @return the registered sound instance
     */
    public static <T extends CustomSound> T registerSound(String key, Function<NamespacedKey, T> factory) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);

        T sound = factory.apply(identifier);
        REGISTRY.register(sound);
        KrimsonPlugin.getInstance().getLogger().info("Registered sound: " + key);

        return (T) REGISTRY.getOrThrow(sound.getKey());
    }

    private Sounds() {
        /* This utility class should not be instantiated */
    }
}
