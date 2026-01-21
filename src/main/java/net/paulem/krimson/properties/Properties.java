package net.paulem.krimson.properties;

import lombok.Getter;
import org.bukkit.block.Block;

public abstract class Properties {
    @Getter
    protected final PDCWrapper container;

    protected Properties(Block block) {
        this.container = new PDCWrapper(block);
    }
}
