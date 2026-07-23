package net.paulem.krimson.utils;

import net.paulem.krimson.pdc.DataTypes;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PersistentDataUtils {
    private static final int CHUNK_MIN_XZ = 0;
    /**
     * The maximum X and Z coordinate of any block inside a chunk.
     */
    private static final int CHUNK_MAX_XZ = (2 << 3) - 1;
    /**
     * Regex used to identify valid CustomBlockData keys
     */
    private static final Pattern KEY_REGEX = Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");
    /**
     * Whether WorldInfo#getMinHeight() method exists. In some very specific versions, it's directly declared in World.
     */
    private static final boolean HAS_MIN_HEIGHT_METHOD;

    static {
        boolean tmpHasMinHeightMethod = false;
        try {
            // Usually declared in WorldInfo, which World extends - except for some very specific versions
            World.class.getMethod("getMinHeight");
            tmpHasMinHeightMethod = true;
        } catch (final ReflectiveOperationException ignored) {
        }
        HAS_MIN_HEIGHT_METHOD = tmpHasMinHeightMethod;
    }

    public static <P, C> PersistentDataType<P, C> getCorrespondType(C constable) {
        if (constable instanceof Byte) return (PersistentDataType<P, C>) PersistentDataType.BYTE;
        if (constable instanceof Short) return (PersistentDataType<P, C>) PersistentDataType.SHORT;
        if (constable instanceof Integer) return (PersistentDataType<P, C>) PersistentDataType.INTEGER;
        if (constable instanceof Long) return (PersistentDataType<P, C>) PersistentDataType.LONG;
        if (constable instanceof Float) return (PersistentDataType<P, C>) PersistentDataType.FLOAT;
        if (constable instanceof Double) return (PersistentDataType<P, C>) PersistentDataType.DOUBLE;
        if (constable instanceof Boolean) return (PersistentDataType<P, C>) PersistentDataType.BOOLEAN;
        if (constable instanceof String) return (PersistentDataType<P, C>) PersistentDataType.STRING;
        if (constable instanceof byte[]) return (PersistentDataType<P, C>) PersistentDataType.BYTE_ARRAY;
        if (constable instanceof int[]) return (PersistentDataType<P, C>) PersistentDataType.INTEGER_ARRAY;
        if (constable instanceof long[]) return (PersistentDataType<P, C>) PersistentDataType.LONG_ARRAY;

        PersistentDataType<P, C> corresponds = (PersistentDataType<P, C>) DataTypes.corresponds(constable.getClass());
        if (corresponds != null) return corresponds;

        throw new IllegalArgumentException("Unsupported type: " + constable.getClass().getName());
    }

    /**
     * Gets a String-based {@link NamespacedKey} that consists of the block's relative coordinates within its chunk<br>
     * From CustomBlockData by mfnalex
     *
     * @param block block
     * @return NamespacedKey consisting of the block's relative coordinates within its chunk
     */
    @NotNull
    public static String getKey(@NotNull final Block block) {
        final int x = block.getX() & 0x000F;
        final int y = block.getY();
        final int z = block.getZ() & 0x000F;
        return "x" + x + "y" + y + "z" + z;
    }

    @NotNull
    public static NamespacedKey getKey(@NotNull final Plugin plugin, @NotNull final Block block) {
        return new NamespacedKey(plugin, getKey(block));
    }

    /**
     * Gets the block represented by the given {@link NamespacedKey} in the given {@link Chunk}
     */
    @Nullable
    public static Block getBlockFromKey(final NamespacedKey key, final Chunk chunk) {
        final int[] coords = parseBlockKeyFast(key.getKey());
        if (coords == null) return null;

        final int x = coords[0];
        final int y = coords[1];
        final int z = coords[2];

        if ((x < CHUNK_MIN_XZ || x > CHUNK_MAX_XZ)
                || (z < CHUNK_MIN_XZ || z > CHUNK_MAX_XZ)
                || (y < getWorldMinHeight(chunk.getWorld()) || y > chunk.getWorld().getMaxHeight() - 1)) {
            return null;
        }
        return chunk.getBlock(x, y, z);
    }

    /**
     * High-performance, zero-allocation parser for "x{num}y{num}z{num}" format.
     * Replaces regex
     */
    private static int @Nullable [] parseBlockKeyFast(String key) {
        if (key == null || key.isEmpty() || key.charAt(0) != 'x') return null;

        int yPos = -1;
        int zPos = -1;

        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == 'y' && yPos == -1) {
                yPos = i;
            } else if (c == 'z' && yPos != -1) {
                zPos = i;
                break;
            }
        }

        if (yPos == -1 || zPos == -1) return null;

        try {
            // Guard against negative X or Z which your format prohibits
            if (key.charAt(1) == '-' || key.charAt(zPos + 1) == '-') {
                return null;
            }

            // Zero-allocation character sequence parsing
            int x = Integer.parseInt(key, 1, yPos, 10);
            int y = Integer.parseInt(key, yPos + 1, zPos, 10);
            int z = Integer.parseInt(key, zPos + 1, key.length(), 10);

            return new int[]{x, y, z};
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Returns a {@link Set} of all blocks in this {@link Chunk} containing Custom Block Data matching the given {@link NamespacedKey}'s namespace<br>
     * From CustomBlockData by mfnalex
     *
     * @param namespace Namespace
     * @param chunk     Chunk
     * @return A {@link Set} containing all blocks in this chunk containing Custom Block Data created by the given plugin
     */
    @NotNull
    public static Set<Block> getBlocksWithCustomData(final @NotNull Chunk chunk, final @NotNull NamespacedKey namespace, @Nullable final Predicate<Block> filter) {
        final PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();
        return chunkPDC.getKeys()
                .stream()
                .filter(key -> key.getNamespace().equals(namespace.getNamespace()) && (filter == null || filter.test(getBlockFromKey(key, chunk))))
                .map(key -> getBlockFromKey(key, chunk))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a Set of all blocks in this chunk containing Custom Block Data created by the given plugin
     *
     * @param plugin Plugin
     * @param chunk  Chunk
     * @return A Set containing all blocks in this chunk containing Custom Block Data created by the given plugin
     */
    @NotNull
    public static Set<Block> getBlocksWithCustomData(final Plugin plugin, final Chunk chunk, @Nullable final Predicate<Block> filter) {
        final NamespacedKey dummy = new NamespacedKey(plugin, "dummy");
        return getBlocksWithCustomData(chunk, dummy, filter);
    }

    /**
     * Returns the given {@link World}'s minimum build height, or 0 if not supported in this Bukkit version
     */
    public static int getWorldMinHeight(final World world) {
        if (HAS_MIN_HEIGHT_METHOD) {
            return world.getMinHeight();
        } else {
            return -64; // The default value for the minimum build height in Minecraft is -64, which is the same as the lowest Y coordinate in the Overworld.
        }
    }
}
