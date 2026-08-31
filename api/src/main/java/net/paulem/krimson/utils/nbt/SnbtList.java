package net.paulem.krimson.utils.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Liste NBT (inclut les tableaux typés {@code [B;]}, {@code [I;]}, {@code [L;]}).
 */
public final class SnbtList implements SnbtTag {
    private final List<SnbtTag> values = new ArrayList<>();

    public void add(SnbtTag tag) {
        values.add(tag);
    }

    public List<SnbtTag> values() {
        return Collections.unmodifiableList(values);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public SnbtTag get(int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    public String getString(int index) {
        return get(index) instanceof SnbtValue value ? value.asString() : "";
    }

    public float getFloat(int index) {
        return get(index) instanceof SnbtValue value ? value.asFloat() : 0f;
    }

    public double getDouble(int index) {
        return get(index) instanceof SnbtValue value ? value.asDouble() : 0d;
    }

    public SnbtCompound getCompound(int index) {
        return get(index) instanceof SnbtCompound compound ? compound : new SnbtCompound();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
