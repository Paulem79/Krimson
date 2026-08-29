package net.paulem.krimson.blocks.custom;

import org.bukkit.block.Block;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.blocks.noteblock.NoteBlockCustomBlock;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;

public class CustomBlockTypeChecker {
    private final Block block;
    private final PDCWrapper properties;

    public CustomBlockTypeChecker(Block block) {
        this.block = block;
        this.properties = new PDCWrapper(block);
    }

    public boolean isNoteBlock() {
        return properties.has(Keys.NOTE_BLOCK);
    }

    public boolean isLightBlock() {
        return properties.has(Keys.EMITTING_LIGHT_LEVEL);
    }

    public boolean isInventoryBlock() {
        return properties.has(Keys.INVENTORY_SIZE) &&
                properties.has(Keys.INVENTORY_TITLE) &&
                properties.has(Keys.INVENTORY_DATA);
    }

    public CustomBlock get() {
        if (KrimsonAPI.isCustomBlock(block)) {
            // Checked first: the note block backend renders through the blockstate, so it must not be
            // reconstructed as a display based block.
            if (isNoteBlock()) {
                return new NoteBlockCustomBlock(block);
            } else if (isLightBlock()) {
                return new LightBlock(block);
            } else if (isInventoryBlock()) {
                return new InventoryCustomBlock(block);
            }
            return new CustomBlock(block);
        }

        return null;
    }
}
