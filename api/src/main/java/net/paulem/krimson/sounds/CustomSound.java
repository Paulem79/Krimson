package net.paulem.krimson.sounds;

import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a custom sound registered through the Krimson API.
 * Follows the same pattern as {@link net.paulem.krimson.items.CustomItem}
 * and {@link net.paulem.krimson.models.BlockDisplayModel}.
 *
 * <p>The sound file (.ogg) should be placed in the plugin's resources at
 * {@code assets/krimson/sounds/<key>.ogg} and will be automatically
 * included in the generated resource pack.</p>
 */
public class CustomSound implements RegistryKey<NamespacedKey> {

    private final NamespacedKey key;
    private final String soundCategory;
    private final boolean stream;
    private final float volume;
    private final float pitch;

    public CustomSound(NamespacedKey key) {
        this(key, "record", true, 1.0f, 1.0f);
    }

    public CustomSound(NamespacedKey key, String soundCategory, boolean stream, float volume, float pitch) {
        this.key = key;
        this.soundCategory = soundCategory;
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Gets the resource pack namespace and key for the sound event.
     *
     * @return "namespace:key" format, e.g. "krimson:test_sound"
     */
    public String getSoundKey() {
        return key.getNamespace() + ":" + key.getKey();
    }

    /**
     * Gets the expected path of the .ogg file inside the resource pack.
     *
     * @return e.g. "krimson:sounds/test_sound"
     */
    public String getSoundPath() {
        return key.getNamespace() + ":sounds/" + key.getKey();
    }

    /**
     * Gets the Minecraft sound category (record, master, music, weather, blocks, etc.).
     */
    public String getSoundCategory() {
        return soundCategory;
    }

    /**
     * Whether this sound should be streamed (true for long audio like music).
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * Gets the default volume (0.0–1.0).
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Gets the default pitch (0.5–2.0).
     */
    public float getPitch() {
        return pitch;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }
}
