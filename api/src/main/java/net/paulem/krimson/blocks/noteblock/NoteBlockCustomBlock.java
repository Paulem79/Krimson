package net.paulem.krimson.blocks.noteblock;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.utils.CustomBlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.jetbrains.annotations.Nullable;

/**
 * A custom block rendered by the note block itself instead of by an {@code ItemDisplay}.
 *
 * <p>The block in the world really is a {@link Material#NOTE_BLOCK}, pinned to the blockstate
 * {@link NoteBlockStates} allocated for this key. The resource pack maps that state to this block's model
 * (see {@code writeNoteBlockStates} in {@code ResourcePack.kt}), so there is no entity to spawn, tick,
 * light or clean up - lighting, occlusion and persistence all come from vanilla.
 *
 * <p>The price is that vanilla note block mechanics would rewrite the state; {@code NoteBlockListener}
 * suppresses them and re-implements them on top of the block PDC.
 *
 * <p>Caveat: the blockstate is still a note block, so collision, hardness, tool and step sounds stay the
 * note block's whatever the model looks like. Models should be full cubes.
 */
public class NoteBlockCustomBlock extends CustomBlock {
    private static final String REGISTRY_REFERENCE_ERROR_MESSAGE = "You must clone this registry instance of the custom block before editing it.";

    /** The blockstate this block renders through, or {@code null} for a block whose key is gone. */
    @Getter
    @Nullable
    private final NoteBlockState noteState;

    /**
     * Registry template constructor. Allocates (or recovers) this key's blockstate.
     */
    public NoteBlockCustomBlock(NamespacedKey key, NamespacedKey dropIdentifier) {
        super(key, dropIdentifier, Material.NOTE_BLOCK);

        this.noteState = NoteBlockStates.allocate(key);
    }

    /**
     * Reconstruction constructor, used on chunk load. Also heals a block whose blockstate drifted away from
     * the allocated one.
     */
    public NoteBlockCustomBlock(Block block) {
        super(block);

        this.noteState = NoteBlockStates.stateOf(getKey());
        heal();
    }

    @Override
    public CustomBlock copyOf() {
        NoteBlockCustomBlock copy = new NoteBlockCustomBlock(this.getKey(), this.getDropIdentifier());

        copy.registryReference = false;
        copy.setMeta(this.getMeta());

        return copy;
    }

    @Override
    public void spawn(Location blockLoc) {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (blockLoc.getWorld() == null || noteState == null) {
            return;
        }

        Block target = blockLoc.getBlock();
        // applyPhysics = false: neighbours must not be told, or vanilla would try to recompute the state
        // we just wrote.
        target.setBlockData(noteState.toBlockData(), false);

        registerLive(target);
        getProperties().getContainer().set(Keys.NOTE_BLOCK, (byte) 1);
    }

    /**
     * No display entity to spawn - the note block is the model. Overridden (rather than left to the base
     * class) because the base constructor calls this during reconstruction.
     */
    @Override
    public void spawnDisplay(Location blockLoc) {
        // Intentionally empty
    }

    /** Nothing to respawn. */
    @Override
    public void tickSync() {
        // Intentionally empty
    }

    /** Nothing to despawn. */
    @Override
    public void onUnload() {
        // Intentionally empty
    }

    @Override
    public void tickAsync() {
        Preconditions.checkState(!isRegistryReference(), REGISTRY_REFERENCE_ERROR_MESSAGE);

        if (getBlock().getType() != Material.NOTE_BLOCK) {
            KrimsonPlugin.getScheduler().runTask(() ->
                    CustomBlockUtils.handleBlockSuppression(getBlock(), null)
            );
        }
    }

    /**
     * Restores the allocated blockstate if the block in the world drifted away from it - after a state
     * remap, or after something bypassed the listener.
     */
    public void heal() {
        Block block = getBlock();

        if (noteState == null || block == null || block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        if (block.getBlockData() instanceof NoteBlock data && !noteState.matches(data)) {
            block.setBlockData(noteState.toBlockData(), false);
        }
    }

    /**
     * Called when the redstone power reaching this block changes. Vanilla would flip the {@code powered}
     * blockstate here; that state carries the block's identity instead, so the change is reported through
     * this hook and the blockstate is left alone.
     */
    public void onPowered(boolean powered) {
        // Default implementation does nothing
    }
}
