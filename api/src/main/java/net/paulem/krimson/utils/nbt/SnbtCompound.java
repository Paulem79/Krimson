package net.paulem.krimson.utils.nbt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Compound NBT : équivalent interne de {@code CompoundTag}, sans dépendance NMS.
 */
public final class SnbtCompound implements SnbtTag {
    private final Map<String, SnbtTag> values = new LinkedHashMap<>();

    public void put(String key, SnbtTag tag) {
        values.put(key, tag);
    }

    public void putString(String key, String value) {
        values.put(key, SnbtValue.ofString(value));
    }

    public void putInt(String key, int value) {
        values.put(key, SnbtValue.ofNumber(value));
    }

    public void remove(String key) {
        values.remove(key);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public Map<String, SnbtTag> values() {
        return Collections.unmodifiableMap(values);
    }

    public SnbtTag get(String key) {
        return values.get(key);
    }

    /**
     * Recherche une clé en ignorant le préfixe {@code minecraft:} éventuel,
     * les composants d'items étant écrits avec ou sans namespace selon la version.
     */
    public SnbtTag getNamespaced(String key) {
        SnbtTag direct = values.get(key);
        if (direct != null) return direct;
        return values.get(key.contains(":") ? key.substring(key.indexOf(':') + 1) : "minecraft:" + key);
    }

    public boolean containsNamespaced(String key) {
        return getNamespaced(key) != null;
    }

    public String getString(String key) {
        return get(key) instanceof SnbtValue value ? value.asString() : "";
    }

    public int getInt(String key) {
        return get(key) instanceof SnbtValue value ? value.asInt() : 0;
    }

    public float getFloat(String key) {
        return get(key) instanceof SnbtValue value ? value.asFloat() : 0f;
    }

    public boolean getBoolean(String key) {
        return get(key) instanceof SnbtValue value && value.asBoolean();
    }

    public SnbtCompound getCompound(String key) {
        return get(key) instanceof SnbtCompound compound ? compound : new SnbtCompound();
    }

    public SnbtList getList(String key) {
        return get(key) instanceof SnbtList list ? list : new SnbtList();
    }

    public boolean isCompound(String key) {
        return get(key) instanceof SnbtCompound;
    }

    public boolean isList(String key) {
        return get(key) instanceof SnbtList;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
