package net.paulem.krimson.resourcepack;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.javalin.Javalin;
import lombok.Getter;
import net.mcbrawls.inject.javalin.InjectJavalinFactory;
import net.mcbrawls.inject.spigot.InjectSpigot;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimson.resourcepack.creator.ResourcePackKt;
import net.radstevee.packed.core.pack.PackFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.paulem.krimson.Krimson;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

public class ResourcePackHosting implements Listener {
    public final Map<String, File> versionToFileMap = new HashMap<>();

    @Getter
    private boolean canPlayersJoin = false;
    @Getter
    private Javalin javalin;

    public ResourcePackHosting() {
    }

    public void aggregate(String version, File zipFile) {
        if(versionToFileMap.containsKey(version)) {
            return;
        }

        versionToFileMap.put(version, zipFile);

        // TODO : Make krimson-pack a constant
        javalin.get("/krimson-pack-" + version, (ctx -> {
            ctx.result(Files.readAllBytes(zipFile.toPath()));
        }));
    }

    public void start() {
        javalin = InjectJavalinFactory.create(InjectSpigot.INSTANCE);

        Krimson.getInstance().getLogger().info("Javalin initialized");

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

        Player player = event.getPlayer();
        ProtocolVersion version = KrimsonPlugin.getViaAPI().getPlayerProtocolVersion(player);
        Set<String> includedVersions = version.getIncludedVersions();

        @Nullable String selectedVersion = null;
        for (String ver : versionToFileMap.keySet()) {
            if (includedVersions.stream().anyMatch(v -> v.toLowerCase().contains(ver.toLowerCase()))) {
                selectedVersion = ver;
                break;
            }
        }

        if (selectedVersion == null) {
            selectedVersion = version.getName();
            aggregate(selectedVersion, ResourcePackKt.main(KrimsonPlugin.getInstance().getDataFolder(), DynamicPackResolver.getFromVersionName(selectedVersion)));
            KrimsonPlugin.getInstance().getLogger().info("Generated resource pack for version " + selectedVersion + " with pack format " + DynamicPackResolver.getFromVersionName(selectedVersion));
        }

        // TODO : Make krimson-pack a constant + Customize message + Make force configurable
        player.addResourcePack(UUID.nameUUIDFromBytes(selectedVersion.getBytes()), "http://localhost:" + Bukkit.getPort() + "/krimson-pack-" + selectedVersion, createSha1(versionToFileMap.get(selectedVersion)), ChatColor.GREEN + "Krimson Resource Pack", true);
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
