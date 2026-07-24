package net.paulem.krimson.ui.font;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.paulem.krimson.ui.CustomUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a custom font-based UI element that can render large background textures
 * using Minecraft's font renderer system, similar to Nova's approach.
 *
 * This allows creating custom GUIs by defining overly large characters that render
 * as background textures, which can be positioned using negative space characters.
 */
@ApiStatus.Experimental
public class CustomFontUI extends CustomUI {
    @Getter
    private final String fontKey;

    @Getter
    private final String backgroundCharacter;

    @Getter
    private String contentText;

    @Getter
    private final int width;

    @Getter
    private final int height;

    @Getter
    private final int ascent;

    /**
     * Creates a custom font UI for rendering background textures.
     *
     * @param key The namespaced key for this UI
     * @param fontKey The font key to use (e.g., "krimson:custom_gui")
     * @param backgroundCharacter The character that renders the background texture
     * @param contentText The text content to display over the background
     * @param width The width of the background texture
     * @param height The height of the background texture
     * @param ascent The vertical offset (ascent) for positioning
     */
    public CustomFontUI(NamespacedKey key, String fontKey, String backgroundCharacter,
                       String contentText, int width, int height, int ascent) {
        super(key, UIType.FONT, contentText, true, fontKey);

        Preconditions.checkArgument(ascent <= height, "Ascent cannot be greater than height");

        this.fontKey = fontKey;
        this.backgroundCharacter = backgroundCharacter;
        this.contentText = contentText;
        this.width = width;
        this.height = height;
        this.ascent = ascent;
    }

    /**
     * Simplified constructor for common use cases.
     */
    public CustomFontUI(NamespacedKey key, String fontKey, String backgroundCharacter,
                       String contentText) {
        this(key, fontKey, backgroundCharacter, contentText, 176, 135, 13);
    }

    @Override
    public void display(Player player) {
        // Use Adventure API for proper component handling
        Component component = Component.text()
                .append(Component.text(backgroundCharacter).font(Key.key(fontKey)))
                .append(Component.text(contentText).font(Key.key("default")))
                .build();

        // Use Adventure Title API
        net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                component,
                Component.empty()
        );
        player.showTitle(title);
    }

    private @Nullable BossBar bossBar;

    public void displayBossBar(Player player, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        // Use Adventure API for proper component handling
        Component component = Component.text()
                .append(Component.text(backgroundCharacter).font(Key.key(fontKey)))
                .append(Component.text(contentText).font(Key.key("default")))
                .build();

        this.bossBar = BossBar.bossBar(component, progress, color, overlay);
        player.showBossBar(this.bossBar);
    }

    @Override
    public void hide(Player player) {
        player.resetTitle();

        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    @Override
    public void updateText(String newText) {
        // In a real implementation, you'd track active displays and update them
        // For now, this would require displaying again
        this.contentText = newText;
    }

    /**
     * Gets the expected path for the background texture.
     * @return e.g., "krimson:textures/gui/mana_bar.png"
     */
    public String getTexturePath() {
        return getKey().getNamespace() + ":textures/gui/" + getKey().getKey() + ".png";
    }

    /**
     * Gets the font JSON configuration for packed resource pack generation.
     * @return Font provider JSON configuration
     */
    public String getFontProviderJson() {
        return String.format(
            "{\n" +
            "  \"providers\": [\n" +
            "    {\n" +
            "      \"type\": \"bitmap\",\n" +
            "      \"file\": \"%s\",\n" +
            "      \"chars\": [\"%s\"],\n" +
            "      \"height\": %d,\n" +
            "      \"ascent\": %d\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            getTexturePath().replace(":", "/"),
            backgroundCharacter,
            height,
            ascent
        );
    }
}