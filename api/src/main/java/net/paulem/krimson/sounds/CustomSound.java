package net.paulem.krimson.sounds;

import lombok.Getter;
import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
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
    @Getter
    private final NamespacedKey key;
    /**
     * -- GETTER --
     *  Gets the Minecraft sound category (record, master, music, weather, blocks, etc.).
     */
    @Getter
    private final SoundCategory soundCategory;
    /**
     * -- GETTER --
     *  Whether this sound should be streamed (true for long audio like music).
     */
    @Getter
    private final boolean stream;
    /**
     * -- GETTER --
     *  Gets the default volume (0.0–1.0).
     */
    @Getter
    private final float volume;
    /**
     * -- GETTER --
     *  Gets the default pitch (0.5–2.0).
     */
    @Getter
    private final float pitch;

    public CustomSound(NamespacedKey key) {
        this(key, SoundCategory.RECORDS, true, 1.0f, 1.0f);
    }

    public CustomSound(NamespacedKey key, SoundCategory soundCategory, boolean stream, float volume, float pitch) {
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
}
