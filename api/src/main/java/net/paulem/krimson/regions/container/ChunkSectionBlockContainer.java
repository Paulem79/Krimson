package net.paulem.krimson.regions.container;

import lombok.Getter;
import net.paulem.krimson.regions.BlockHolder;
import net.paulem.krimson.regions.container.blocks.DynamicSectionContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkSectionBlockContainer {

    private static final int SIZE = 16;
    private static final int VOLUME = SIZE * SIZE * SIZE;

    @Getter
    private final ChunkBlockContainer parent;
    private final DynamicSectionContainer blocks;
    @Getter
    private final int sectionY;
    private int validCount;

    private ChunkSectionBlockContainer(ChunkBlockContainer parent, int sectionY) {
        this.parent = parent;
        this.blocks = DynamicSectionContainer.create(VOLUME);
        this.sectionY = sectionY;
    }

    public static ChunkSectionBlockContainer of(ChunkBlockContainer parent, int sectionY) {
        return new ChunkSectionBlockContainer(parent, sectionY);
    }

    public <T> T getBlock(int x, int y, int z) {
        BlockHolder<T> holder = (BlockHolder<T>) blocks.get(key(x, y, z));
        return holder == null ? null : holder.getData();
    }

    public <T> void setBlock(int x, int y, int z, T block) {
        BlockHolder<?> oldHolder = blocks.get(key(x, y, z));
        blocks.set(key(x, y, z), new BlockHolder<>(x, y, z, block));

        if (oldHolder == null) {
            validCount++;
        }
    }

    public void removeBlock(int x, int y, int z) {
        BlockHolder<?> holder = blocks.get(key(x, y, z));

        if (holder == null) {
            return;
        }

        blocks.set(key(x, y, z), null);
        validCount--;

        notifyParent();
    }

    public void clear() {
        for (int index = 0; index < VOLUME; index++) {
            blocks.set(index, null);
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
            BlockHolder<?> holder = blocks.get(index);

            if (holder == null) {
                continue;
            }

            holders.add(holder);
        }

        return holders;
    }
}