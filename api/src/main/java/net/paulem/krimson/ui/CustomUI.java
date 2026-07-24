package net.paulem.krimson.ui;

import lombok.Getter;
import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Base class representing a custom UI element in the Krimson API.
 * Can be displayed as a bossbar, action bar, or title.
 */
public abstract class CustomUI implements RegistryKey<NamespacedKey> {
    @Getter
    private final NamespacedKey key;

    @Getter
    private final UIType type;

    @Getter
    private final String displayText;

    private final boolean useNegativeSpaceFont;

    private final String fontKey;

    public boolean useNegativeSpaceFont() {
        return useNegativeSpaceFont;
    }

    public String getFontKey() {
        return fontKey;
    }

    protected CustomUI(NamespacedKey key, UIType type, String displayText, boolean useNegativeSpaceFont, String fontKey) {
        this.key = key;
        this.type = type;
        this.displayText = displayText;
        this.useNegativeSpaceFont = useNegativeSpaceFont;
        this.fontKey = fontKey;
    }

    /**
     * Displays this UI element to the specified player.
     * @param player The player to display to
     */
    public abstract void display(Player player);

    /**
     * Hides this UI element from the specified player.
     * @param player The player to hide from
     */
    public abstract void hide(Player player);

    /**
     * Updates the display text of this UI element.
     * @param newText The new display text
     */
    public abstract void updateText(String newText);

    /**
     * Gets the resource pack namespace and key for any associated textures.
     * @return "namespace:key" format
     */
    public String getTextureKey() {
        return key.getNamespace() + ":" + key.getKey();
    }

    public enum UIType {
        BOSSBAR,
        ACTIONBAR,
        TITLE,
        FONT
    }
}