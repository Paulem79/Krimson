package ovh.paulem.krimson.blocks.list;

import ovh.paulem.krimson.blocks.CustomBlock;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collector;

public class CustomBlocks<T extends CustomBlock> extends CopyOnWriteArraySet<T> {
    public<C extends CustomBlock> CustomBlocks<C> getAll(Class<C> clazz) {
        return parallelStream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(collector());
    }

    private static <C extends CustomBlock> Collector<C, ?, CustomBlocks<C>> collector() {
        return Collector.of(
                CustomBlocks::new,
                CustomBlocks::add,
                (left, right) -> { left.addAll(right); return left; }
        );
    }
}
