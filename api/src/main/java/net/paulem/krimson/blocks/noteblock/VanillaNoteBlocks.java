package net.paulem.krimson.blocks.noteblock;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.NotePlayEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla note block, re-implemented on top of the block PDC.
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
        if (block.getBlockData() instanceof NoteBlock data && vanilla.matches(data)) {
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
     * Plays the note, the way vanilla would.
     *
     * @return {@code true} if a sound was played
     */
    public static boolean playNote(Block block, int note) {
        NoteBlockInstrument nmsInstrument = resolveNmsInstrument(block);

        // Vanilla stays silent when covered, unless a mob head sits on top
        Block above = block.getRelative(BlockFace.UP);
        if (!nmsInstrument.worksAboveNoteBlock() && !above.getType().isAir()) {
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

        Instrument playedInstrument = event.getInstrument();
        int playedNote = event.getNote().getId();

        Sound sound = getSound(playedInstrument);
        if (sound != null) {
            boolean tunable = isTunable(playedInstrument);
            float pitch = tunable ? (float) Math.pow(2.0D, (playedNote - 12) / 12.0D) : 1.0F;

            World world = block.getWorld();
            world.playSound(
                    block.getLocation().add(0.5D, 0.5D, 0.5D),
                    sound, SoundCategory.RECORDS, 3.0F, pitch
            );

            if (tunable) {
                world.spawnParticle(
                        Particle.NOTE,
                        block.getX() + 0.5D, block.getY() + 1.2D, block.getZ() + 0.5D,
                        0, playedNote / 24.0D, 0.0D, 0.0D, 1.0D
                );
            }
        }

        return true;
    }

    private static NoteBlockInstrument resolveNmsInstrument(Block block) {
        // Block above (e.g. mob heads)
        Block above = block.getRelative(BlockFace.UP);
        NoteBlockInstrument aboveInst = ((CraftBlock) above).getNMS().instrument();
        if (aboveInst.worksAboveNoteBlock()) {
            return aboveInst;
        }

        // Block below (e.g. sand, wood, stone)
        Block below = block.getRelative(BlockFace.DOWN);
        NoteBlockInstrument belowInst = ((CraftBlock) below).getNMS().instrument();

        return belowInst.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : belowInst;
    }

    @Nullable
    private static Instrument toBukkit(NoteBlockInstrument instrument) {
        return NoteBlockState.byVanillaName(instrument.getSerializedName());
    }

    @Nullable
    private static Sound getSound(Instrument instrument) {
        return instrument.getSound();
    }

    private static boolean isTunable(Instrument instrument) {
        return switch (instrument) {
            case ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD -> false;
            default -> true;
        };
    }

    private static int clampNote(int note) {
        return Math.floorMod(note, NoteBlockState.NOTES);
    }
}