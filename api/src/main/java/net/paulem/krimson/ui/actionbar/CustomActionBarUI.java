package net.paulem.krimson.ui.actionbar;

import lombok.Getter;
import net.paulem.krimson.ui.CustomUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Represents a custom action bar UI element with support for negative space fonts.
 */
public class CustomActionBarUI extends CustomUI {
    @Getter
    private final int durationTicks;

    public CustomActionBarUI(NamespacedKey key, String displayText, boolean useNegativeSpaceFont,
                           String fontKey, int durationTicks) {
        super(key, UIType.ACTIONBAR, displayText, useNegativeSpaceFont, fontKey);
        this.durationTicks = durationTicks;
    }

    public CustomActionBarUI(NamespacedKey key, String displayText) {
        this(key, displayText, false, "minecraft:default", 100); // 5 seconds by default
    }

    @Override
    public void display(Player player) {
        String message = getDisplayText();
        if (useNegativeSpaceFont()) {
            message = "{\"font\":\"" + getFontKey() + "\"}" + message;
        }
        player.sendActionBar(message);

        // For persistent display, you would need to resend periodically
        // This could be handled by a scheduler in a more advanced implementation
    }

    @Override
    public void hide(Player player) {
        // Action bars are temporary, just send an empty message to clear
        player.sendActionBar("");
    }

    @Override
    public void updateText(String newText) {
        // Action bars are not persistent, so updating requires displaying again
        // In a real implementation, you'd track active displays and update them
    }

    /**
     * Gets the expected path for any background texture.
     * @return e.g. "krimson:textures/gui/my_actionbar.png"
     */
    public String getTexturePath() {
        return getKey().getNamespace() + ":textures/gui/" + getKey().getKey() + ".png";
    }
}