package net.paulem.krimson;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.retrooper.packetevents.PacketEvents;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.paulem.arcana.ArcanaAPI;
import net.paulem.krimson.config.KrimsonConfig;
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
    private static KrimsonConfig configuration;

    @Getter
    private ArcanaAPI<KrimsonPlugin<T>> arcanaAPI;

    @Override
    public void onLoad() {
        super.onLoad();

        // Must be built/loaded in onLoad(): PacketEvents needs to inject before the
        // server starts accepting connections.
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        instance = this;

        PacketEvents.getAPI().init();

        arcanaAPI = new ArcanaAPI<>(this);
        arcanaAPI.init();

        saveDefaultConfig();
        configuration = arcanaAPI.loadConfig(KrimsonConfig.class, getConfig());

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

    @Override
    public void onDisable() {
        super.onDisable();

        PacketEvents.getAPI().terminate();
    }

    public abstract void initBlocks();

    public abstract void initItems();

    public abstract void initModels();

    public abstract void initMobs();

    public abstract void initSounds();

    public abstract void initUIs();
}
