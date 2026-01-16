package net.paulem.krimson.regions.container;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import net.paulem.krimson.regions.BlockHolder;
import net.paulem.krimson.regions.ChunkKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkBlockContainer {
    private static final int SECTION_SIZE = 16;
    private static final int MIN_HEIGHT = -64;
    private static final int MAX_HEIGHT = 256;
    private static final int SECTION_COUNT = (MAX_HEIGHT - MIN_HEIGHT) / SECTION_SIZE;

    @Getter
    private final WorldBlockContainer parent;
    @Getter
    private final ChunkKey chunkKey;
    private final ChunkSectionBlockContainer[] sections;

    private ChunkBlockContainer(WorldBlockContainer parent, ChunkKey chunkKey) {
        this.parent = parent;
        this.chunkKey = chunkKey;
        this.sections = new ChunkSectionBlockContainer[SECTION_COUNT];
    }

    public static ChunkBlockContainer of(WorldBlockContainer parent, ChunkKey chunkKey) {
        return new ChunkBlockContainer(parent, chunkKey);
    }

    public ChunkSectionBlockContainer getSection(int sectionY) {
        return sections[sectionY];
    }

    public ChunkSectionBlockContainer getOrCreateSection(int sectionY) {
        ChunkSectionBlockContainer section = getSection(sectionY);

        if (section == null) {
            section = ChunkSectionBlockContainer.of(this, sectionY);
            sections[sectionY] = section;
        }

        return section;
    }

    public void removeSection(ChunkSectionBlockContainer section) {
        sections[section.getSectionY()] = null;
        notifyParent();
    }

    public void clear() {
        for (ChunkSectionBlockContainer section : sections) {
            section.clear();
        }
    }

    @Nullable
    public <T> T getBlock(int x, int y, int z) {
        if (y < MIN_HEIGHT || y > MAX_HEIGHT) {
            return null;
        }

        ChunkSectionBlockContainer section = getOrCreateSection(y / SECTION_SIZE);

        if (section == null) {
            return null;
        }

        return section.getBlock(x, y, z);
    }

    public <T> void setBlock(int x, int y, int z, T block) {
        if (y < MIN_HEIGHT || y > MAX_HEIGHT) {
            return;
        }

        ChunkSectionBlockContainer section = getOrCreateSection(y / SECTION_SIZE);

        if (section == null) {
            return;
        }

        section.setBlock(x, y, z, block);
    }

    public void removeBlock(int x, int y, int z) {
        if (y < MIN_HEIGHT || y > MAX_HEIGHT) {
            return;
        }

        ChunkSectionBlockContainer section = getOrCreateSection(y / SECTION_SIZE);

        if (section == null) {
            return;
        }

        section.removeBlock(x, y, z);
    }

    private void notifyParent() {
        for (ChunkSectionBlockContainer section : sections) {
            if (section != null) {
                return;
            }
        }

        parent.removeChunkContainer(chunkKey);
    }

    public Collection<BlockHolder<?>> getAllBlocks() {
        List<BlockHolder<?>> holders = new ArrayList<>();

        for (ChunkSectionBlockContainer section : sections) {
            if (section == null) {
                continue;
            }

            holders.addAll(section.getAllBlocks());
        }

        return holders;
    }
}