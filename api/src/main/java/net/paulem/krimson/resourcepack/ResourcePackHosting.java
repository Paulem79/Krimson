package net.paulem.krimson.resourcepack;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.javalin.Javalin;
import lombok.Getter;
import net.mcbrawls.inject.javalin.InjectJavalinFactory;
import net.mcbrawls.inject.spigot.InjectSpigot;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.resourcepack.creator.ResourcePackKt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

public class ResourcePackHosting implements Listener {
    private static final String RESOURCE_PACK_PREFIX = "krimson-pack-";

    public final Map<String, File> versionToFileMap = new HashMap<>();

    @Getter
    private boolean canPlayersJoin = false;
    @Getter
    private Javalin javalin;

    public ResourcePackHosting() {
        // This constructor is intentionally empty
    }

    public void aggregate(String version, File zipFile) {
        if (!versionToFileMap.containsKey(version)) {
            versionToFileMap.put(version, zipFile);
        }
    }

    public void start() {
        javalin = InjectJavalinFactory.create(InjectSpigot.INSTANCE, config -> {
            // 1. Migration des événements dans le bloc config
            config.events.serverStarted(() -> {
                KrimsonPlugin.getInstance().getLogger().info("Javalin started");
                canPlayersJoin = true;
            });

            config.events.serverStartFailed(() -> {
                KrimsonPlugin.getInstance().getLogger().info("Javalin failed to start");
                canPlayersJoin = true;
            });

            // 2. Création d'une route unique paramétrée au lieu de routes dynamiques
            config.routes.get("/" + RESOURCE_PACK_PREFIX + "{version}", ctx -> {
                String requestedVersion = ctx.pathParam("version");
                File packFile = versionToFileMap.get(requestedVersion);

                if (packFile != null && packFile.exists()) {
                    // Optimisation : Passer un InputStream directement à ctx.result()
                    // consomme beaucoup moins de RAM que Files.readAllBytes() pour les fichiers ZIP.
                    ctx.result(new FileInputStream(packFile));
                } else {
                    ctx.status(404).result("Resource pack not found");
                }
            });
        });

        KrimsonPlugin.getInstance().getLogger().info("Javalin initialized");

        javalin.start();
    }

    public void stop() {
        if (javalin != null) {
            javalin.stop();
            KrimsonPlugin.getInstance().getLogger().info("Javalin stopped");
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

        // Resource pack URL and configuration
        final String resourcePackUrl = "http://localhost:" + Bukkit.getPort() + "/" + RESOURCE_PACK_PREFIX + selectedVersion;
        final String resourcePackMessage = ChatColor.GREEN + "Krimson Resource Pack";
        final boolean forceResourcePack = true; // Can be made configurable via config.yml

        player.addResourcePack(
                UUID.nameUUIDFromBytes(selectedVersion.getBytes()),
                resourcePackUrl,
                createSha1(versionToFileMap.get(selectedVersion)),
                resourcePackMessage,
                forceResourcePack);
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