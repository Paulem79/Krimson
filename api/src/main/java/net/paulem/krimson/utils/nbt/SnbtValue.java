package net.paulem.krimson.utils.nbt;

/**
 * Valeur scalaire NBT : nombre, booléen ou chaîne.
 */
public final class SnbtValue implements SnbtTag {
    private final Object value;
    private final boolean string;

    private SnbtValue(Object value, boolean string) {
        this.value = value;
        this.string = string;
    }

    public static SnbtValue ofString(String value) {
        return new SnbtValue(value, true);
    }

    public static SnbtValue ofNumber(Number value) {
        return new SnbtValue(value, false);
    }

    public static SnbtValue ofBoolean(boolean value) {
        return new SnbtValue(value ? (byte) 1 : (byte) 0, false);
    }

    public boolean isString() {
        return string;
    }

    public boolean isNumber() {
        return !string;
    }

    public Object value() {
        return value;
    }

    public String asString() {
        return String.valueOf(value);
    }

    public Number asNumber() {
        if (value instanceof Number number) return number;
        try {
            return Double.parseDouble(asString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int asInt() {
        return asNumber().intValue();
    }

    public float asFloat() {
        return asNumber().floatValue();
    }

    public double asDouble() {
        return asNumber().doubleValue();
    }

    public boolean asBoolean() {
        return asNumber().intValue() != 0;
    }

    @Override
    public String toString() {
        return asString();
    }
}
