package net.paulem.krimson.regions.container.blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.paulem.krimson.regions.BlockHolder;

public interface SectionContainer {

    BlockHolder<?> get(int position);

    void set(int position, BlockHolder<?> holder);

    Int2ObjectOpenHashMap<BlockHolder<?>> getAll();

    default int size() {
        return getAll().size();
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    default void copyTo(SectionContainer target) {
        for (Int2ObjectMap.Entry<BlockHolder<?>> entry : target.getAll().int2ObjectEntrySet()) {
            target.set(entry.getIntKey(), entry.getValue());
        }
    }
}