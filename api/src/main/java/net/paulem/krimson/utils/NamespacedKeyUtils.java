package net.paulem.krimson.utils;

import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.NamespacedKey;

public class NamespacedKeyUtils {
    public static NamespacedKey none() {
        return new NamespacedKey(KrimsonPlugin.getInstance(), "none");
    }
}
