package ovh.paulem.krimson.resourcepack;

import io.javalin.Javalin;
import lombok.Getter;
import net.mcbrawls.inject.javalin.InjectJavalinFactory;
import net.mcbrawls.inject.spigot.InjectSpigot;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ovh.paulem.krimson.Krimson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;

public class ResourcePackHosting implements Listener {
    public static final UUID RESOURCE_PACK_ID = UUID.randomUUID();

    @Getter
    private boolean canPlayersJoin = false;
    @Getter
    private final File zipFile;
    @Getter
    private Javalin javalin;

    public ResourcePackHosting(File zipFile) {
        this.zipFile = zipFile;
    }

    public void start() {
        javalin = InjectJavalinFactory.create(InjectSpigot.INSTANCE);

        Krimson.getInstance().getLogger().info("Javalin initialized");
        Krimson.getInstance().getLogger().info(RESOURCE_PACK_ID.toString());

        javalin.events(eventConfig -> {
            eventConfig.serverStarted(() -> {
                Krimson.getInstance().getLogger().info("Javalin started");
                canPlayersJoin = true;
            });

            eventConfig.serverStartFailed(() -> {
                Krimson.getInstance().getLogger().info("Javalin failed to start");
                canPlayersJoin = true;
            });
        });

        javalin.get("/" + RESOURCE_PACK_ID, (ctx -> {
            ctx.result(Files.readAllBytes(zipFile.toPath()));
        }));

        javalin.start();
    }

    public void stop() {
        if (javalin != null) {
            javalin.stop();
            Krimson.getInstance().getLogger().info("Javalin stopped");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!canPlayersJoin) {
            event.getPlayer().kickPlayer("Server is not ready yet !");
            return;
        }

        event.getPlayer().addResourcePack(RESOURCE_PACK_ID, "http://localhost:25565/" + RESOURCE_PACK_ID, createSha1(zipFile), ChatColor.GREEN + "THIT resource pack", true);
    }

    public byte[] createSha1(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream fis = new FileInputStream(file)) {
                int n = 0;
                byte[] buffer = new byte[8192];
                while (n != -1) {
                    n = fis.read(buffer);
                    if (n > 0) {
                        digest.update(buffer, 0, n);
                    }
                }
            }
            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
