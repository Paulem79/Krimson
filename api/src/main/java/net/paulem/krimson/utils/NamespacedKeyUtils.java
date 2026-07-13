package net.paulem.krimson.utils;

import net.paulem.krimson.common.KrimsonPlugin;
import org.bukkit.NamespacedKey;
import net.paulem.krimson.KrimsonAPI;

public class NamespacedKeyUtils {
    public static NamespacedKey none() {
        return new NamespacedKey(KrimsonPlugin.getInstance(), "none");
    }
}
