package net.paulem.krimson;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Allow to access the plugin instance from the sub api modules.
 */
public abstract class KrimsonPlugin<T extends KrimsonPlugin<T>> extends JavaPlugin {
    @Getter
    private static KrimsonPlugin<?> instance;

    @Getter
    private static ViaAPI<Player> viaAPI;

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

        // Optional ViaVersion support for protocol version handling
        // ViaVersion allows the plugin to work with different Minecraft client versions
        try {
            viaAPI = Via.getAPI();
            getLogger().info("ViaVersion support enabled");
        } catch (Exception e) {
            getLogger().warning("ViaVersion not found - protocol version features will be limited");
            viaAPI = null;
        }

        getLogger().info("KrimsonPlugin instantiated!");
    }

    public abstract void initBlocks();

    public abstract void initItems();

    public abstract void initModels();

    public abstract void initSounds();

    public abstract void initEntities();
}
