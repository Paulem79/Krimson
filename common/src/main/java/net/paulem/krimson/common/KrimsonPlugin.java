package net.paulem.krimson.common;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import lombok.Getter;
import net.paulem.krimson.utils.NativeUtil;
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

        NativeUtil.init();

        // TODO : Optional ViaVersion support
        viaAPI = Via.getAPI();

        getLogger().info("KrimsonPlugin instantiated!");
    }
}
