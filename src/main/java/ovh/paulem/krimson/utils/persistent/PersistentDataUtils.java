package ovh.paulem.krimson.utils.persistent;

import org.bukkit.persistence.PersistentDataType;

public class PersistentDataUtils {
    public static<P, C> PersistentDataType<P, C> getCorrespondType(C constable) {
        if (constable instanceof Byte) return (PersistentDataType<P, C>) PersistentDataType.BYTE;
        if (constable instanceof Short) return (PersistentDataType<P, C>) PersistentDataType.SHORT;
        if (constable instanceof Integer) return (PersistentDataType<P, C>) PersistentDataType.INTEGER;
        if (constable instanceof Long) return (PersistentDataType<P, C>) PersistentDataType.LONG;
        if (constable instanceof Float) return (PersistentDataType<P, C>) PersistentDataType.FLOAT;
        if (constable instanceof Double) return (PersistentDataType<P, C>) PersistentDataType.DOUBLE;
        if (constable instanceof Boolean) return (PersistentDataType<P, C>) PersistentDataType.BOOLEAN;
        if (constable instanceof String) return (PersistentDataType<P, C>) PersistentDataType.STRING;
        if (constable instanceof byte[]) return (PersistentDataType<P, C>) PersistentDataType.BYTE_ARRAY;
        if (constable instanceof int[]) return (PersistentDataType<P, C>) PersistentDataType.INTEGER_ARRAY;
        if (constable instanceof long[]) return (PersistentDataType<P, C>) PersistentDataType.LONG_ARRAY;
        throw new IllegalArgumentException("Unsupported type: " + constable.getClass().getName());
    }
}
