package net.paulem.krimson.ui.bossbar;

import lombok.Getter;
import net.paulem.krimson.ui.CustomUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a custom bossbar UI element with support for negative space fonts.
 */
public class CustomBossBarUI extends CustomUI {
    @Getter
    private final BarColor color;

    @Getter
    private final BarStyle style;

    private final boolean useCustomTexture;

    public boolean useCustomTexture() {
        return useCustomTexture;
    }

    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    public CustomBossBarUI(NamespacedKey key, String displayText, BarColor color, BarStyle style,
                          boolean useNegativeSpaceFont, String fontKey, boolean useCustomTexture) {
        super(key, UIType.BOSSBAR, displayText, useNegativeSpaceFont, fontKey);
        this.color = color;
        this.style = style;
        this.useCustomTexture = useCustomTexture;
    }

    public CustomBossBarUI(NamespacedKey key, String displayText) {
        this(key, displayText, BarColor.WHITE, BarStyle.SOLID, false, "minecraft:default", false);
    }

    @Override
    public void display(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(getKey().getNamespace());
        if (plugin == null) return;

        String titleText = getDisplayText();
        if (useNegativeSpaceFont()) {
            titleText = "{\"font\":\"" + getFontKey() + "\"}" + titleText;
        }
        BossBar bossBar = Bukkit.createBossBar(titleText, color, style);

        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        playerBossBars.put(player.getUniqueId(), bossBar);
    }

    @Override
    public void hide(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    @Override
    public void updateText(String newText) {
        String titleText = newText;
        if (useNegativeSpaceFont()) {
            titleText = "{\"font\":\"" + getFontKey() + "\"}" + titleText;
        }
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.setTitle(titleText);
        }
    }

    /**
     * Sets the progress of the bossbar.
     * @param progress Value between 0.0 and 1.0
     */
    public void setProgress(double progress) {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.setProgress(progress);
        }
    }

    /**
     * Sets the color of the bossbar.
     * @param color The new color
     */
    public void setColor(BarColor color) {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.setColor(color);
        }
    }

    /**
     * Sets the style of the bossbar.
     * @param style The new style
     */
    public void setStyle(BarStyle style) {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.setStyle(style);
        }
    }

    /**
     * Gets the expected path of the custom texture inside the resource pack.
     * @return e.g. "krimson:textures/gui/my_bossbar.png"
     */
    public String getTexturePath() {
        return getKey().getNamespace() + ":textures/gui/" + getKey().getKey() + ".png";
    }
}