package net.paulem.krimson.models;

import net.paulem.krimson.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;

import java.util.List;

public interface Model<T extends Display> extends RegistryKey<NamespacedKey> {
    List<T> spawn(Location location);

    void playAnimation(World world, String instanceId, String animationName);
}
