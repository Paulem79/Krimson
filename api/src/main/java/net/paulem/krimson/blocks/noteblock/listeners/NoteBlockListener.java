package net.paulem.krimson.blocks.noteblock.listeners;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.blocks.noteblock.NoteBlockCustomBlock;
import net.paulem.krimson.blocks.noteblock.VanillaNoteBlocks;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Suppresses vanilla note block state changes and replays the resulting behaviour by hand.
 *
 * <p>Custom blocks are stored <i>in</i> the note block's blockstate, so any vanilla mechanic that rewrites
 * {@code instrument}, {@code note} or {@code powered} would silently turn one custom block into another.
 * Every such mechanic is therefore cancelled here, and re-implemented on top of the block PDC through
 * {@link VanillaNoteBlocks} - so a real note block still cycles its pitch when clicked, still picks up the
 * instrument of the block below it, and still plays when a redstone signal reaches it.
 *
 * <p>Registered last, so the interaction handlers of {@code CustomBlockActionListener} and
 * {@code BlockItemHandlerListener} still get their turn first.
 */
public class NoteBlockListener implements Listener {

    /**
     * Vanilla recomputes {@code powered} and {@code instrument} from neighbour updates. Cancelling the
     * update is what protects every custom block's state; the same event doubles as the redstone edge
     * detector, since a note block only ever sees a power change through a neighbour update.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        event.setCancelled(true);

        boolean powered = block.isBlockIndirectlyPowered();
        if (powered == VanillaNoteBlocks.isPowered(block)) {
            return;
        }

        VanillaNoteBlocks.setPowered(block, powered);

        NoteBlockCustomBlock customBlock = customBlockAt(block);
        if (customBlock != null) {
            customBlock.onPowered(powered);
            return;
        }

        if (powered) {
            VanillaNoteBlocks.playNote(block, VanillaNoteBlocks.getNote(block));
        }
    }

    /**
     * Vanilla cycles the note on right click. Denied unconditionally - the blockstate must not move - and
     * replayed against the PDC for real note blocks.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.useInteractedBlock() == Event.Result.DENY) {
            // Already handled - BlockItemHandlerListener placing a custom block against this one, typically.
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        // Denying the block interaction still lets the held item be used, so placing a block against a note
        // block keeps working.
        event.setUseInteractedBlock(Event.Result.DENY);

        if (KrimsonAPI.isCustomBlockFromWatcher(block)) {
            // CustomBlockActionListener already ran onInteract for it; nothing vanilla to replay.
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItem(EquipmentSlot.HAND);
        if (player.isSneaking() && held != null && !held.getType().isAir()) {
            return; // Sneak + item means the player wants to place, exactly like vanilla.
        }

        event.setUseItemInHand(Event.Result.DENY);

        VanillaNoteBlocks.playNote(block, VanillaNoteBlocks.cycleNote(block));
    }

    /**
     * A freshly placed note block carries an instrument derived from its neighbours; pin it back to the
     * reserved state so it does not accidentally look like a custom block.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.NOTE_BLOCK || KrimsonAPI.isCustomBlock(block)) {
            return;
        }

        VanillaNoteBlocks.pin(block);
        VanillaNoteBlocks.setNote(block, 0);
        VanillaNoteBlocks.setPowered(block, block.isBlockIndirectlyPowered());
    }

    /** A block appearing above or below a note block changes that note block's instrument in vanilla. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNeighbourPlaced(BlockPlaceEvent event) {
        repinNeighbours(event.getBlockPlaced());
    }

    /** Keeps the chunk PDC from accumulating entries for note blocks that no longer exist. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK || KrimsonAPI.isCustomBlock(block)) {
            return;
        }

        VanillaNoteBlocks.clear(block);
    }

    /** Same as above, for a block disappearing next to a note block. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNeighbourBroken(BlockBreakEvent event) {
        repinNeighbours(event.getBlock());
    }

    /**
     * Vanilla re-picks the instrument of a note block whenever its vertical neighbour changes, through
     * {@code NoteBlock#updateShape} - a path that never fires {@link BlockPhysicsEvent}, so it cannot be
     * cancelled. Custom blocks repair themselves on their sync tick; real note blocks have no tick, so they
     * are re-pinned here, or they would drift onto a state that belongs to a custom block and start
     * rendering as it.
     */
    private static void repinNeighbours(Block block) {
        KrimsonPlugin.getScheduler().runTask(() -> {
            for (int dy : new int[]{-1, 1}) {
                Block neighbour = block.getRelative(0, dy, 0);
                if (neighbour.getType() == Material.NOTE_BLOCK && !KrimsonAPI.isCustomBlock(neighbour)) {
                    VanillaNoteBlocks.pin(neighbour);
                }
            }
        });
    }

    private static NoteBlockCustomBlock customBlockAt(Block block) {
        return KrimsonAPI.customBlocks.getBlockAt(block) instanceof NoteBlockCustomBlock customBlock
                ? customBlock
                : null;
    }
}
