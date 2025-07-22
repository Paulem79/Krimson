package ovh.paulem.krimson.blocks.list;

import ovh.paulem.krimson.blocks.CustomBlock;

import java.util.LinkedList;
import java.util.stream.Collector;

public class CustomBlocksList<T extends CustomBlock> extends LinkedList<T> {
    public<C extends CustomBlock> CustomBlocksList<C> getAll(Class<C> clazz) {
        return stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(collector());
    }

    private static <C extends CustomBlock> Collector<C, ?, CustomBlocksList<C>> collector() {
        return Collector.of(
                CustomBlocksList::new,
                CustomBlocksList::add,
                (left, right) -> { left.addAll(right); return left; }
        );
    }
}
