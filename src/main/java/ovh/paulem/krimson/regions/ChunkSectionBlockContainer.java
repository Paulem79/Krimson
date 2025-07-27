package ovh.paulem.krimson.regions;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkSectionBlockContainer {

    private static final int SIZE = 16;
    private static final int VOLUME = SIZE * SIZE * SIZE;

    @Getter
    private final ChunkBlockContainer parent;
    private final BlockHolder<?>[] blocks;
    @Getter
    private final int sectionY;
    private int validCount;

    private ChunkSectionBlockContainer(ChunkBlockContainer parent, int sectionY) {
        this.parent = parent;
        this.blocks = new BlockHolder[VOLUME];
        this.sectionY = sectionY;
    }

    public static ChunkSectionBlockContainer of(ChunkBlockContainer parent, int sectionY) {
        return new ChunkSectionBlockContainer(parent, sectionY);
    }

    public <T> T getBlock(int x, int y, int z) {
        BlockHolder<T> holder = (BlockHolder<T>) blocks[key(x, y, z)];
        return holder == null ? null : holder.getData();
    }

    public <T> void setBlock(int x, int y, int z, T block) {
        BlockHolder<?> oldHolder = blocks[key(x, y, z)];
        blocks[key(x, y, z)] = new BlockHolder<>(x, y, z, block);

        if (oldHolder == null) {
            validCount++;
        }
    }

    public void removeBlock(int x, int y, int z) {
        BlockHolder<?> holder = blocks[key(x, y, z)];

        if (holder == null) {
            return;
        }

        blocks[key(x, y, z)] = null;
        validCount--;

        notifyParent();
    }

    public void clear() {
        for (int index = 0; index < VOLUME; index++) {
            blocks[index] = null;
        }

        validCount = 0;
    }

    // Utility methods

    private int key(int x, int y, int z) {
        x = x % SIZE;
        y = y % SIZE;
        z = z % SIZE;

        return Math.abs(x + (y * SIZE) + (z * SIZE * SIZE));
    }

    private void notifyParent() {
        if (validCount == 0) {
            parent.removeSection(this);
        }
    }

    public Collection<BlockHolder<?>> getAllBlocks() {
        List<BlockHolder<?>> holders = new ArrayList<>();

        for (int index = 0; index < VOLUME; index++) {
            BlockHolder<?> holder = blocks[index];

            if (holder == null) {
                continue;
            }

            holders.add(holder);
        }

        return holders;
    }
}