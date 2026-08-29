package net.paulem.krimson.blocks.noteblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.block.NotePlayEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla note block, re-implemented on top of the block PDC.
 *
 * <p>Custom blocks live in the note block's blockstate ({@link NoteBlockState}), so vanilla is not allowed
 * to touch that state any more: {@code NoteBlockListener} cancels every note block physics update and every
 * note block interaction. That would leave real note blocks inert, so their behaviour is rebuilt here -
 * the blockstate stays pinned to {@link NoteBlockState#vanilla()} and the note the player cycled through
 * and the last known redstone power are stored in the PDC instead.
 *
 * <p>The instrument is never stored: exactly like vanilla, it is derived on demand from the block above
 * (mob heads) and the block below.
 */
public final class VanillaNoteBlocks {
    private VanillaNoteBlocks() {
        /* This utility class should not be instantiated */
    }

    /** Forces a real note block back onto the reserved vanilla blockstate. */
    public static void pin(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        NoteBlockState vanilla = NoteBlockState.vanilla();
        if (block.getBlockData() instanceof org.bukkit.block.data.type.NoteBlock data && vanilla.matches(data)) {
            return;
        }

        block.setBlockData(vanilla.toBlockData(), false);
    }

    /** The note a player would see on this note block, 0-24. */
    public static int getNote(Block block) {
        return clampNote(new PDCWrapper(block).getOrDefault(Keys.VANILLA_NOTE, 0));
    }

    public static void setNote(Block block, int note) {
        new PDCWrapper(block).set(Keys.VANILLA_NOTE, clampNote(note));
    }

    /** Advances the note one step, wrapping at 25 like vanilla, and returns the new value. */
    public static int cycleNote(Block block) {
        int note = (getNote(block) + 1) % NoteBlockState.NOTES;
        setNote(block, note);

        return note;
    }

    /** The last redstone power state seen for this block. Also used for custom blocks, as an edge detector. */
    public static boolean isPowered(Block block) {
        return new PDCWrapper(block).getOrDefault(Keys.VANILLA_POWERED, (byte) 0) != 0;
    }

    public static void setPowered(Block block, boolean powered) {
        new PDCWrapper(block).set(Keys.VANILLA_POWERED, (byte) (powered ? 1 : 0));
    }

    /** Drops the emulation data of a note block that is going away. */
    public static void clear(Block block) {
        PDCWrapper properties = new PDCWrapper(block);
        properties.getContainer().remove(Keys.VANILLA_NOTE.key());
        properties.getContainer().remove(Keys.VANILLA_POWERED.key());
    }

    /**
     * Resolves the instrument this note block would sound with, following vanilla: a mob head directly above
     * wins, otherwise the block below decides, and a block below that only works above falls back to harp.
     */
    @Nullable
    public static Instrument resolveInstrument(Block block) {
        return toBukkit(resolveNmsInstrument(block));
    }

    /**
     * Plays the note, the way vanilla would. Fires {@link NotePlayEvent} first so plugins listening for
     * vanilla note blocks keep working, and honours its cancellation and its instrument/note overrides.
     *
     * @return {@code true} if a sound was played
     */
    public static boolean playNote(Block block, int note) {
        NoteBlockInstrument nmsInstrument = resolveNmsInstrument(block);

        // Vanilla stays silent when the note block is covered, unless the instrument is one of those that
        // only exist because something is sitting on top of it.
        if (!nmsInstrument.worksAboveNoteBlock() && !block.getRelative(0, 1, 0).getType().isAir()) {
            return false;
        }

        Instrument instrument = toBukkit(nmsInstrument);
        if (instrument == null) {
            return false;
        }

        NotePlayEvent event = new NotePlayEvent(block, instrument, new Note(clampNote(note)));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        int playedNote = event.getNote().getId();
        NoteBlockInstrument playedInstrument = toNms(event.getInstrument(), nmsInstrument);

        float pitch = playedInstrument.isTunable() ? (float) Math.pow(2.0D, (playedNote - 12) / 12.0D) : 1.0F;
        Holder<SoundEvent> sound = playedInstrument.getSoundEvent();

        ServerLevel level = ((CraftWorld) block.getWorld()).getHandle();
        level.playSound(
                (net.minecraft.world.entity.player.Player) null,
                block.getX() + 0.5D, block.getY() + 0.5D, block.getZ() + 0.5D,
                sound, SoundSource.RECORDS, 3.0F, pitch
        );

        if (playedInstrument.isTunable()) {
            World world = block.getWorld();
            world.spawnParticle(
                    Particle.NOTE,
                    block.getX() + 0.5D, block.getY() + 1.2D, block.getZ() + 0.5D,
                    0, playedNote / 24.0D, 0.0D, 0.0D, 1.0D
            );
        }

        return true;
    }

    private static NoteBlockInstrument resolveNmsInstrument(Block block) {
        ServerLevel level = ((CraftWorld) block.getWorld()).getHandle();
        BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());

        NoteBlockInstrument above = level.getBlockState(pos.above()).instrument();
        if (above.worksAboveNoteBlock()) {
            return above;
        }

        NoteBlockInstrument below = level.getBlockState(pos.below()).instrument();

        return below.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : below;
    }

    @Nullable
    private static Instrument toBukkit(NoteBlockInstrument instrument) {
        return NoteBlockState.byVanillaName(instrument.getSerializedName());
    }

    private static NoteBlockInstrument toNms(Instrument instrument, NoteBlockInstrument fallback) {
        String name = NoteBlockState.vanillaName(instrument);
        if (name == null) {
            return fallback;
        }

        for (NoteBlockInstrument candidate : NoteBlockInstrument.values()) {
            if (candidate.getSerializedName().equals(name)) {
                return candidate;
            }
        }

        return fallback;
    }

    private static int clampNote(int note) {
        return Math.floorMod(note, NoteBlockState.NOTES);
    }
}
