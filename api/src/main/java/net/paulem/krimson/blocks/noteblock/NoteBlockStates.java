package net.paulem.krimson.blocks.noteblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assigns one note block blockstate to each note block backed custom block, and remembers the assignment
 * across restarts in {@code <dataFolder>/noteblock_states.json}.
 *
 * <p>Persisting matters because the world only stores the blockstate: if indices shifted when a block was
 * added or removed from {@code initBlocks()}, every already-placed block would silently start rendering as
 * something else. Indices are therefore assigned on first registration and never reused.
 *
 * <p>Identity itself stays PDC backed ({@code Keys.IDENTIFIER}), so a block whose blockstate drifted is
 * still recoverable - {@link NoteBlockCustomBlock} heals it on chunk load.
 */
public final class NoteBlockStates {
    private NoteBlockStates() {
        /* This utility class should not be instantiated */
    }

    private static final String FILE_NAME = "noteblock_states.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<NamespacedKey, Integer> BY_KEY = new LinkedHashMap<>();
    private static final Map<Integer, NamespacedKey> BY_INDEX = new HashMap<>();

    @Nullable
    private static File file;

    /**
     * Reads the persisted allocations. Must run before {@code initBlocks()}, since registering a note block
     * allocates a state.
     */
    public static void load(File dataFolder) {
        BY_KEY.clear();
        BY_INDEX.clear();

        file = new File(dataFolder, FILE_NAME);

        if (!file.exists()) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(entry.getKey());
                if (key == null) {
                    KrimsonPlugin.getInstance().getLogger().warning("Invalid note block state key: " + entry.getKey());
                    continue;
                }

                int index = entry.getValue().getAsInt();
                if (index < 0 || index > NoteBlockState.maxIndex()) {
                    KrimsonPlugin.getInstance().getLogger().warning("Out of range note block state index for " + key + ": " + index);
                    continue;
                }

                NamespacedKey previous = BY_INDEX.putIfAbsent(index, key);
                if (previous != null) {
                    KrimsonPlugin.getInstance().getLogger().warning("Note block state " + index + " is claimed by both " + previous + " and " + key + " - ignoring the latter.");
                    continue;
                }

                BY_KEY.put(key, index);
            }
        } catch (IOException | RuntimeException e) {
            KrimsonPlugin.getInstance().getLogger().warning("Failed to read " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Writes the allocations back out. Call once all blocks have been registered. */
    public static void save() {
        if (file == null) {
            return;
        }

        JsonObject root = new JsonObject();
        for (Map.Entry<NamespacedKey, Integer> entry : BY_KEY.entrySet()) {
            root.addProperty(entry.getKey().toString(), entry.getValue());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            KrimsonPlugin.getInstance().getLogger().warning("Failed to write " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /** Returns the state already assigned to {@code key}, assigning a fresh one if there is none. */
    public static NoteBlockState allocate(NamespacedKey key) {
        Integer existing = BY_KEY.get(key);
        if (existing != null) {
            return NoteBlockState.fromIndex(existing);
        }

        for (int index : NoteBlockState.allocationOrder()) {
            if (BY_INDEX.containsKey(index)) {
                continue;
            }

            BY_INDEX.put(index, key);
            BY_KEY.put(key, index);

            return NoteBlockState.fromIndex(index);
        }

        throw new IllegalStateException("Out of note block states: all " + NoteBlockState.allocationOrder().size() + " are taken.");
    }

    @Nullable
    public static NoteBlockState stateOf(@Nullable NamespacedKey key) {
        Integer index = key == null ? null : BY_KEY.get(key);
        return index == null ? null : NoteBlockState.fromIndex(index);
    }

    @Nullable
    public static NamespacedKey keyAt(NoteBlockState state) {
        return BY_INDEX.get(state.toIndex());
    }

    /** All current allocations, in registration order. Read by the resource pack generator. */
    public static Map<NamespacedKey, NoteBlockState> allocations() {
        Map<NamespacedKey, NoteBlockState> allocations = new LinkedHashMap<>();
        for (Map.Entry<NamespacedKey, Integer> entry : BY_KEY.entrySet()) {
            allocations.put(entry.getKey(), NoteBlockState.fromIndex(entry.getValue()));
        }

        return Collections.unmodifiableMap(allocations);
    }
}
