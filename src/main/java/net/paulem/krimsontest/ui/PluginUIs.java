package net.paulem.krimsontest.ui;

import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.ui.UIRegistry;
import net.paulem.krimson.ui.bossbar.CustomBossBarUI;
import net.paulem.krimson.ui.actionbar.CustomActionBarUI;
import net.paulem.krimson.ui.title.CustomTitleUI;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

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

    public static final CustomBossBarUI MANA_BAR = UIRegistry.registerUI("mana_bar", key ->
            new CustomBossBarUI(
                    key,
                    "Mana: 100/100",  // Initial text
                    BarColor.BLUE,     // Blue color for mana
                    BarStyle.SEGMENTED_10,  // 10 segments
                    true,              // Use negative space font
                    "krimson:spaces",  // Negative space font key
                    true               // Enable custom texture
            )
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering UIs...");
        UIRegistry.REGISTRY.freeze();
    }
}