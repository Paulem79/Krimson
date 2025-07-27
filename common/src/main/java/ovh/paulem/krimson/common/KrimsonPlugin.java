package ovh.paulem.krimson.common;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Allow to access the plugin instance from the sub api modules.
 */
public abstract class KrimsonPlugin<T extends KrimsonPlugin<T>> extends JavaPlugin {
    @Getter
    protected static KrimsonPlugin<?> instance;

    @Getter
    private static TaskScheduler scheduler;

    @Getter
    private static FileConfiguration configuration;

    @Override
    public void onEnable() {
        super.onEnable();

        instance = this;

        saveDefaultConfig();
        configuration = getConfig();

        scheduler = UniversalScheduler.getScheduler(this);

        getLogger().info("KrimsonPlugin instantiated!");
    }
}
