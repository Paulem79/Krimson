package net.paulem.krimson.blocks.noteblock;

import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.properties.PDCWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
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
     * Resolves the instrument this note block would sound with, following vanilla: a mob head directly above
     * wins, otherwise the block below decides.
     */
    @Nullable
    public static Instrument resolveInstrument(Block block) {
        // 1. Check top block for mob heads
        Block above = block.getRelative(BlockFace.UP);
        Instrument headInstrument = getHeadInstrument(above.getType());
        if (headInstrument != null) {
            return headInstrument;
        }

        // 2. Check block below
        Block below = block.getRelative(BlockFace.DOWN);
        return getInstrumentFromBelow(below.getType());
    }

    /**
     * Plays the note, the way vanilla would.
     *
     * @return {@code true} if a sound was played
     */
    public static boolean playNote(Block block, int note) {
        Block above = block.getRelative(BlockFace.UP);
        Instrument headInstrument = getHeadInstrument(above.getType());

        // Vanilla stays silent when covered, unless a mob head sits on top
        if (headInstrument == null && !above.getType().isAir()) {
            return false;
        }

        Instrument instrument = resolveInstrument(block);
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
            float pitch = isTunable(playedInstrument) ? (float) Math.pow(2.0D, (playedNote - 12) / 12.0D) : 1.0F;
            World world = block.getWorld();

            world.playSound(
                    block.getLocation().add(0.5D, 0.5D, 0.5D),
                    sound, SoundCategory.RECORDS, 3.0F, pitch
            );

            if (isTunable(playedInstrument)) {
                world.spawnParticle(
                        Particle.NOTE,
                        block.getX() + 0.5D, block.getY() + 1.2D, block.getZ() + 0.5D,
                        0, playedNote / 24.0D, 0.0D, 0.0D, 1.0D
                );
            }
        }

        return true;
    }

    @Nullable
    private static Instrument getHeadInstrument(Material type) {
        return switch (type) {
            case ZOMBIE_HEAD, ZOMBIE_WALL_HEAD -> Instrument.ZOMBIE;
            case SKELETON_SKULL, SKELETON_WALL_SKULL -> Instrument.SKELETON;
            case CREEPER_HEAD, CREEPER_WALL_HEAD -> Instrument.CREEPER;
            case DRAGON_HEAD, DRAGON_WALL_HEAD -> Instrument.DRAGON;
            case WITHER_SKELETON_SKULL, WITHER_SKELETON_WALL_SKULL -> Instrument.WITHER_SKELETON;
            case PIGLIN_HEAD, PIGLIN_WALL_HEAD -> Instrument.PIGLIN;
            case PLAYER_HEAD, PLAYER_WALL_HEAD -> Instrument.CUSTOM_HEAD;
            default -> null;
        };
    }

    private static Instrument getInstrumentFromBelow(Material type) {
        if (Tag.WOODEN_TRAPDOORS.isTagged(type) || Tag.PLANKS.isTagged(type) || Tag.LOGS.isTagged(type)) {
            return Instrument.BASS_GUITAR;
        }
        if (Tag.SAND.isTagged(type) || type == Material.GRAVEL || type == Material.SOUL_SAND) {
            return Instrument.SNARE_DRUM;
        }
        if (Tag.IMPERMEABLE.isTagged(type) || type == Material.GLASS || type == Material.GLASS_PANE) {
            return Instrument.STICKS;
        }
        if (Tag.BASE_STONE_OVERWORLD.isTagged(type) || Tag.ICE.isTagged(type) || type == Material.NETHERRACK || type == Material.OBSIDIAN) {
            return Instrument.BASS_DRUM;
        }

        return switch (type) {
            case GOLD_BLOCK -> Instrument.BELL;
            case CLAY -> Instrument.FLUTE;
            case PACKED_ICE -> Instrument.CHIME;
            case WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL, LIME_WOOL, PINK_WOOL, GRAY_WOOL, LIGHT_GRAY_WOOL, CYAN_WOOL, PURPLE_WOOL, BLUE_WOOL, BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL -> Instrument.GUITAR;
            case BONE_BLOCK -> Instrument.XYLOPHONE;
            case IRON_BLOCK -> Instrument.IRON_XYLOPHONE;
            case SOUL_SOIL -> Instrument.COW_BELL;
            case PUMPKIN -> Instrument.DIDGERIDOO;
            case EMERALD_BLOCK -> Instrument.BIT;
            case HAY_BLOCK -> Instrument.BANJO;
            case GLOWSTONE -> Instrument.PLING;
            default -> Instrument.PIANO; // HARP
        };
    }

    @Nullable
    private static Sound getSound(Instrument instrument) {
        return switch (instrument) {
            case PIANO -> Sound.BLOCK_NOTE_BLOCK_HARP;
            case BASS_DRUM -> Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
            case SNARE_DRUM -> Sound.BLOCK_NOTE_BLOCK_SNARE;
            case STICKS -> Sound.BLOCK_NOTE_BLOCK_HAT;
            case BASS_GUITAR -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case FLUTE -> Sound.BLOCK_NOTE_BLOCK_FLUTE;
            case BELL -> Sound.BLOCK_NOTE_BLOCK_BELL;
            case GUITAR -> Sound.BLOCK_NOTE_BLOCK_GUITAR;
            case CHIME -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case XYLOPHONE -> Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
            case IRON_XYLOPHONE -> Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
            case COW_BELL -> Sound.BLOCK_NOTE_BLOCK_COW_BELL;
            case DIDGERIDOO -> Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO;
            case BIT -> Sound.BLOCK_NOTE_BLOCK_BIT;
            case BANJO -> Sound.BLOCK_NOTE_BLOCK_BANJO;
            case PLING -> Sound.BLOCK_NOTE_BLOCK_PLING;
            case ZOMBIE -> Sound.BLOCK_NOTE_BLOCK_IMITATE_ZOMBIE;
            case SKELETON -> Sound.BLOCK_NOTE_BLOCK_IMITATE_SKELETON;
            case CREEPER -> Sound.BLOCK_NOTE_BLOCK_IMITATE_CREEPER;
            case DRAGON -> Sound.BLOCK_NOTE_BLOCK_IMITATE_ENDER_DRAGON;
            case WITHER_SKELETON -> Sound.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON;
            case PIGLIN -> Sound.BLOCK_NOTE_BLOCK_IMITATE_PIGLIN;
            default -> Sound.BLOCK_NOTE_BLOCK_HARP;
        };
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