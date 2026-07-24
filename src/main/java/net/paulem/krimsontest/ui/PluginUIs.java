package net.paulem.krimsontest.ui;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.ui.UIRegistry;

/**
 * Test plugin UIs — mirrors the pattern of
 * {@link net.paulem.krimsontest.blocks.PluginBlocks},
 * {@link net.paulem.krimsontest.items.PluginItems},
 * {@link net.paulem.krimsontest.models.PluginModels}, and
 * {@link net.paulem.krimsontest.sounds.PluginSounds}.
 */
public class PluginUIs {
    private PluginUIs() {
        /* This utility class should not be instantiated */
    }

    // Font-based mana bar using large background texture (Nova-style approach)
    public static final net.paulem.krimson.ui.font.CustomFontUI FONT_MANA_BAR =
            UIRegistry.registerUI("font_mana_bar", key ->
                    new net.paulem.krimson.ui.font.CustomFontUI(
                            key,
                            "krimson:mana_font",  // Custom font key
                            "",                  // Unicode private use character for background
                            "Mana",                // Text to display
                            100,                   // Width of background texture
                            18,                    // Height of background texture
                            -80                     // Ascent (vertical positioning)
                    )
            );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering UIs...");
        UIRegistry.REGISTRY.freeze();
    }
}