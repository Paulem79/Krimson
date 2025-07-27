package ovh.paulem.krimson.utils;

import org.bukkit.NamespacedKey;
import ovh.paulem.krimson.Krimson;

public class NamespacedKeyUtils {
    public static NamespacedKey none() {
        return new NamespacedKey(Krimson.getInstance(), "none");
    }
}
