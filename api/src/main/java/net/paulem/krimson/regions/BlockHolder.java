package net.paulem.krimson.regions;

import lombok.Getter;

@Getter
public final class BlockHolder<T> {
    private final int x;
    private final int y;
    private final int z;
    private final T data;

    public BlockHolder(int x, int y, int z, T data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
    }
}