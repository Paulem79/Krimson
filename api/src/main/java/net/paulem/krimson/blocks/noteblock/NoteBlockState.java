package net.paulem.krimson.blocks.noteblock;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.type.NoteBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One addressable note block blockstate: {@code instrument} x {@code note} x {@code powered}.
 *
 * <p>Every combination is a distinct client-visible state, which is what makes the note block usable as a
 * carrier for custom blocks: the resource pack maps each state to its own model through
 * {@code assets/minecraft/blockstates/note_block.json}. See {@link NoteBlockStates} for the allocation of
 * those states to custom block keys.
 *
 * <p>The instrument ordering is <b>not</b> the {@link Instrument} enum ordinal. Instruments are ordered by
 * the name vanilla actually serialises them under ({@code harp}, {@code basedrum}, {@code hat}, ...), read
 * back from a real {@link NoteBlock} block data, so the ordering survives a Bukkit enum reordering and the
 * blockstate variant keys never need a hardcoded table.
 */
public record NoteBlockState(Instrument instrument, int note, boolean powered) {
    /** Number of distinct {@code note} values (0-24). */
    public static final int NOTES = 25;

    public NoteBlockState {
        if (note < 0 || note >= NOTES) {
            throw new IllegalArgumentException("Note must be in [0, " + (NOTES - 1) + "], got " + note);
        }
    }

    /**
     * The state a freshly placed vanilla note block carries. Reserved: it is never handed out to a custom
     * block, so real note blocks keep rendering as note blocks.
     */
    public static NoteBlockState vanilla() {
        return new NoteBlockState(Instrument.PIANO, 0, false);
    }

    /** Instruments usable as a carrier, ordered by their vanilla serialised name. */
    public static List<Instrument> instruments() {
        return Instruments.ORDER;
    }

    /** The name vanilla serialises {@code instrument} under, e.g. {@code harp} for {@link Instrument#PIANO}. */
    public static String vanillaName(Instrument instrument) {
        return Instruments.NAMES.get(instrument);
    }

    /** Resolves a vanilla instrument name back to its Bukkit constant, or {@code null}. */
    @Nullable
    public static Instrument byVanillaName(String vanillaName) {
        return Instruments.BY_NAME.get(vanillaName);
    }

    /** Highest valid index, inclusive. */
    public static int maxIndex() {
        return Instruments.ORDER.size() * NOTES * 2 - 1;
    }

    /**
     * The order states are handed out in. The reserved vanilla state is excluded entirely, and the remaining
     * states of the vanilla instrument ({@code harp}) are pushed to the very end so that a note block which
     * somehow escaped state pinning still renders vanilla for as long as possible.
     */
    public static List<Integer> allocationOrder() {
        return Instruments.ALLOCATION_ORDER;
    }

    public static NoteBlockState fromIndex(int index) {
        if (index < 0 || index > maxIndex()) {
            throw new IllegalArgumentException("Note block state index out of range: " + index);
        }

        boolean powered = (index & 1) == 1;
        int rest = index >> 1;
        int note = rest % NOTES;
        int instrumentIndex = rest / NOTES;

        return new NoteBlockState(Instruments.ORDER.get(instrumentIndex), note, powered);
    }

    @Nullable
    public static NoteBlockState fromBlockData(NoteBlock data) {
        if (!Instruments.ORDER.contains(data.getInstrument())) {
            return null;
        }

        return new NoteBlockState(data.getInstrument(), data.getNote().getId(), data.isPowered());
    }

    public int toIndex() {
        int instrumentIndex = Instruments.ORDER.indexOf(instrument);
        if (instrumentIndex < 0) {
            throw new IllegalStateException("Unsupported note block instrument: " + instrument);
        }

        return ((instrumentIndex * NOTES) + note) * 2 + (powered ? 1 : 0);
    }

    public NoteBlock toBlockData() {
        NoteBlock data = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        data.setInstrument(instrument);
        data.setNote(new Note(note));
        data.setPowered(powered);

        return data;
    }

    /** {@code true} if {@code data} already carries exactly this state. */
    public boolean matches(NoteBlock data) {
        return data.getInstrument() == instrument
                && data.getNote().getId() == note
                && data.isPowered() == powered;
    }

    /**
     * The key this state is written under in {@code assets/minecraft/blockstates/note_block.json},
     * e.g. {@code instrument=harp,note=0,powered=false}.
     */
    public String variantString() {
        return "instrument=" + vanillaName(instrument) + ",note=" + note + ",powered=" + powered;
    }

    /** Lazily initialised holder: building block data needs a running server. */
    private static final class Instruments {
        static final List<Instrument> ORDER;
        static final Map<Instrument, String> NAMES;
        static final Map<String, Instrument> BY_NAME;
        static final List<Integer> ALLOCATION_ORDER;

        static {
            Map<Instrument, String> names = new EnumMap<>(Instrument.class);
            Map<String, Instrument> byName = new LinkedHashMap<>();

            for (Instrument instrument : Instrument.values()) {
                String name = serialisedNameOf(instrument);
                if (name == null || byName.containsKey(name)) {
                    // Either the instrument cannot be applied to a note block, or it collapses onto an
                    // instrument we already have - either way it is not a distinct carrier state.
                    continue;
                }

                names.put(instrument, name);
                byName.put(name, instrument);
            }

            List<Instrument> order = new ArrayList<>(names.keySet());
            order.sort(Comparator.comparing(names::get));

            ORDER = List.copyOf(order);
            NAMES = Map.copyOf(names);
            BY_NAME = Map.copyOf(byName);

            Instrument vanillaInstrument = vanilla().instrument();
            int reserved = indexOf(ORDER, vanillaInstrument, vanilla().note(), vanilla().powered());

            List<Integer> allocationOrder = new ArrayList<>();
            List<Integer> vanillaInstrumentStates = new ArrayList<>();
            int max = ORDER.size() * NOTES * 2 - 1;
            for (int index = 0; index <= max; index++) {
                if (index == reserved) {
                    continue;
                }

                if (ORDER.get((index >> 1) / NOTES) == vanillaInstrument) {
                    vanillaInstrumentStates.add(index);
                } else {
                    allocationOrder.add(index);
                }
            }
            allocationOrder.addAll(vanillaInstrumentStates);

            ALLOCATION_ORDER = List.copyOf(allocationOrder);
        }

        private static int indexOf(List<Instrument> order, Instrument instrument, int note, boolean powered) {
            return ((order.indexOf(instrument) * NOTES) + note) * 2 + (powered ? 1 : 0);
        }

        @Nullable
        private static String serialisedNameOf(Instrument instrument) {
            try {
                NoteBlock data = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
                data.setInstrument(instrument);
                return readProperty(data.getAsString(), "instrument");
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** Pulls one property out of {@code minecraft:note_block[instrument=harp,note=0,powered=false]}. */
        @Nullable
        private static String readProperty(String asString, String property) {
            int open = asString.indexOf('[');
            int close = asString.lastIndexOf(']');
            if (open < 0 || close < open) {
                return null;
            }

            for (String part : asString.substring(open + 1, close).split(",")) {
                int equals = part.indexOf('=');
                if (equals > 0 && part.substring(0, equals).trim().equals(property)) {
                    return part.substring(equals + 1).trim();
                }
            }

            return null;
        }
    }
}
