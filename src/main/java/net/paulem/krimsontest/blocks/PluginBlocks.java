package net.paulem.krimsontest.blocks;

import net.paulem.krimson.blocks.Blocks;
import net.paulem.krimson.blocks.custom.InventoryCustomBlock;
import net.paulem.krimson.blocks.noteblock.NoteBlockCustomBlock;
import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.Material;

import java.util.List;

public class PluginBlocks {
    private PluginBlocks() {
        /* This utility class should not be instantiated */
    }

    public static final InventoryCustomBlock TEST = Blocks.register(
            "test_block",
            meta -> {
                meta.setDisplayName("§aTest Block");
                meta.setLore(List.of("§bThis is a test block", "§cfor inventory placement."));
            },
            key -> new InventoryCustomBlock(key, key, Material.OAK_WOOD, 3 * 9, "Inventaire de placement de test")
    );

    /**
     * Same idea as {@link #TEST}, but rendered by a note block blockstate instead of by an item display
     * entity: no entity, vanilla lighting, vanilla persistence.
     */
    public static final NoteBlockCustomBlock NOTE_TEST = Blocks.registerNoteBlock(
            "note_test_block",
            meta -> {
                meta.setDisplayName("§dNote Test Block");
                meta.setLore(List.of("§bA custom block rendered", "§bthrough a noteblock state."));
            }
    );

    public static void init() {
        KrimsonPlugin.getInstance().getLogger().info("Registering blocks...");

        Blocks.REGISTRY.freeze();
    }
}
