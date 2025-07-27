package ovh.paulem.krimson.regions.container.blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import ovh.paulem.krimson.regions.BlockHolder;

public class MapContainer implements SectionContainer {

    private final Int2ObjectOpenHashMap<BlockHolder<?>> map = new Int2ObjectOpenHashMap<>();

    @Override
    public BlockHolder<?> get(int position) {
        return map.get(position);
    }

    @Override
    public void set(int position, BlockHolder<?> holder) {
        map.put(position, holder);
    }

    @Override
    public Int2ObjectOpenHashMap<BlockHolder<?>> getAll() {
        return map.clone();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}