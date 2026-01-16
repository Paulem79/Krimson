package net.paulem.krimson.regions.container.blocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.paulem.krimson.regions.BlockHolder;

// TODO: If you're fancy about it you can also implement a putAll method that pre-expands and yeets it all for a bit more efficiency
public class DynamicSectionContainer implements SectionContainer {
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;

    private final float loadFactor;
    private final int maxSize;

    private SectionContainer currentContainer;
    private boolean expanded = false;

    private DynamicSectionContainer(float loadFactor, int maxSize) {
        this.loadFactor = loadFactor;
        this.maxSize = maxSize;
        this.currentContainer = new MapContainer();
    }

    public static DynamicSectionContainer create(float loadFactor, int maxSize) {
        return new DynamicSectionContainer(loadFactor, maxSize);
    }

    public static DynamicSectionContainer create(int maxSize) {
        return create(DEFAULT_LOAD_FACTOR, maxSize);
    }

    @Override
    public BlockHolder<?> get(int position) {
        return currentContainer.get(position);
    }

    @Override
    public void set(int position, BlockHolder<?> holder) {
        currentContainer.set(position, holder);
        recalculate();
    }

    @Override
    public Int2ObjectOpenHashMap<BlockHolder<?>> getAll() {
        return currentContainer.getAll();
    }

    public void putAll(SectionContainer other) {
        if (!expanded && ((float) (currentContainer.size() + other.size()) / maxSize) >= loadFactor) {
            expand();
        }

        other.getAll().forEach((pos, holder) -> currentContainer.set(pos, holder));
        recalculate();
    }

    private float calculateLoadFactor() {
        return (float) currentContainer.size() / maxSize;
    }

    private void recalculate() {
        if (expanded) {
            return;
        }

        if (calculateLoadFactor() < loadFactor) {
            return;
        }

        expand();
    }

    private void expand() {
        SectionContainer old = currentContainer;
        SectionContainer newContainer = ArraySectionContainer.create(maxSize);

        old.copyTo(newContainer);
        this.currentContainer = newContainer;
        this.expanded = true;
    }
}