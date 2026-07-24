package net.paulem.krimson.ui.title;

import lombok.Getter;
import net.paulem.krimson.ui.CustomUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Represents a custom title UI element with support for negative space fonts.
 */
public class CustomTitleUI extends CustomUI {
    @Getter
    private final String subtitle;

    @Getter
    private final int fadeInTicks;

    @Getter
    private final int stayTicks;

    @Getter
    private final int fadeOutTicks;

    public CustomTitleUI(NamespacedKey key, String titleText, String subtitle,
                        boolean useNegativeSpaceFont, String fontKey,
                        int fadeInTicks, int stayTicks, int fadeOutTicks) {
        super(key, UIType.TITLE, titleText, useNegativeSpaceFont, fontKey);
        this.subtitle = subtitle;
        this.fadeInTicks = fadeInTicks;
        this.stayTicks = stayTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    public CustomTitleUI(NamespacedKey key, String titleText) {
        this(key, titleText, "", false, "minecraft:default", 10, 70, 20);
    }

    @Override
    public void display(Player player) {
        String title = getDisplayText();
        if (useNegativeSpaceFont()) {
            title = "{\"font\":\"" + getFontKey() + "\"}" + title;
        }
        String sub = subtitle;
        if (useNegativeSpaceFont() && !subtitle.isEmpty()) {
            sub = "{\"font\":\"" + getFontKey() + "\"}" + sub;
        }

        player.sendTitle(title, sub, fadeInTicks, stayTicks, fadeOutTicks);
    }

    @Override
    public void hide(Player player) {
        // Titles automatically disappear after their duration
        player.resetTitle();
    }

    @Override
    public void updateText(String newText) {
        // Titles are not persistent, so updating requires displaying again
    }

    /**
     * Gets the expected path for title background texture.
     * @return e.g. "krimson:textures/gui/my_title.png"
     */
    public String getTexturePath() {
        return getKey().getNamespace() + ":textures/gui/" + getKey().getKey() + ".png";
    }
}