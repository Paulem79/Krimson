package net.paulem.krimson.regions.container.blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.paulem.krimson.regions.BlockHolder;

public class ArraySectionContainer implements SectionContainer {

    private final BlockHolder<?>[] contents;
    private int size;

    private ArraySectionContainer(int size) {
        contents = new BlockHolder[size];
    }

    public static ArraySectionContainer create(int size) {
        return new ArraySectionContainer(size);
    }

    @Override
    public BlockHolder<?> get(int position) {
        return contents[position];
    }

    @Override
    public void set(int position, BlockHolder<?> holder) {
        BlockHolder<?> old = get(position);
        contents[position] = holder;

        if (old == null && holder != null) {
            size++;
        } else if (old != null && holder == null) {
            size--;
        }
    }

    @Override
    public Int2ObjectOpenHashMap<BlockHolder<?>> getAll() {
        Int2ObjectOpenHashMap<BlockHolder<?>> result = new Int2ObjectOpenHashMap<>();

        if (size == 0) {
            return result;
        }

        for (int index = 0; index < contents.length; index++) {
            BlockHolder<?> holder = contents[index];

            if (holder == null) {
                continue;
            }

            result.put(index, holder);
        }

        return result;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }
}