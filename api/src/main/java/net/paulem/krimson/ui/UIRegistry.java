package net.paulem.krimson.ui;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.registry.NewFrozenRegistry;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * Registry utility for custom UI elements — mirrors the pattern of
 * {@link net.paulem.krimson.sounds.Sounds},
 * {@link net.paulem.krimson.blocks.Blocks},
 * {@link net.paulem.krimson.items.Items}, and
 * {@link net.paulem.krimson.models.Models}.
 *
 * <p>Usage:
 * <pre>{@code
 * public static final CustomBossBarUI MY_BOSSBAR = UIRegistry.registerUI("my_bossbar", CustomBossBarUI::new);
 * }</pre>
 */
@ApiStatus.Experimental
public class UIRegistry {
    public static final NewFrozenRegistry<CustomUI, NamespacedKey> REGISTRY = new NewFrozenRegistry<>();

    /**
     * Registers a custom UI element with the given key.
     *
     * @param key     the UI name (e.g. "my_bossbar"), will be namespaced with the plugin's namespace
     * @param factory factory function receiving the NamespacedKey, returning a CustomUI
     * @param <T>     the specific CustomUI subtype
     * @return the registered UI instance
     */
    public static <T extends CustomUI> T registerUI(String key, Function<NamespacedKey, T> factory) {
        NamespacedKey identifier = new NamespacedKey(KrimsonPlugin.getInstance(), key);

        T ui = factory.apply(identifier);
        REGISTRY.register(ui);
        KrimsonPlugin.getInstance().getLogger().info("Registered UI: " + key);

        return (T) REGISTRY.getOrThrow(ui.getKey());
    }

    private UIRegistry() {
        /* This utility class should not be instantiated */
    }
}