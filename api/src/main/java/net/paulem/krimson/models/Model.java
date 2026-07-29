package net.paulem.krimson.models;

import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;

public interface Model<T, A1, A2> extends RegistryKey<NamespacedKey> {
    T spawn(Location location);

    void playAnimation(A1 kindaLoc, A2 instance, String animationName);
}
