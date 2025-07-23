package ovh.paulem.krimson.common;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Allow to access the plugin instance from the sub api modules.
 */
public abstract class KrimsonPlugin extends JavaPlugin {
    @Getter
    protected static KrimsonPlugin instance;
}
