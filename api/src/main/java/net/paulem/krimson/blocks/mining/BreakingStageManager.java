package net.paulem.krimson.blocks.mining;

import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Sends the block cracking animation of a server driven mining to every player able to see it.
 */
public final class BreakingStageManager {
    /** Radius, in blocks, in which the cracking animation is shown to other players. */
    public static final double RADIUS = KrimsonPlugin.getConfiguration().getDouble("miningDamageRadius", 16);

    private BreakingStageManager() {
        /* This utility class should not be instantiated */
    }

    public static void sendBlockDamage(Location blockLocation, float progress, double radius) {
        if (blockLocation.getWorld() == null) return;

        Collection<Player> nearbyPlayers = blockLocation.getWorld()
                .getNearbyEntities(blockLocation, radius, radius, radius)
                .stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .toList();

        for (Player player : nearbyPlayers) {
            // progress must be between 0.0f and 1.0f; the location hashcode is used as the transaction id
            // because it is stable for a given block and cheap to compute
            player.sendBlockDamage(blockLocation, Math.clamp(progress, 0.0f, 1.0f), blockLocation.hashCode());
        }
    }

    public static void sendBlockDamage(Location blockLocation, float progress) {
        sendBlockDamage(blockLocation, progress, RADIUS);
    }

    public static void resetBlockDamage(Location blockLocation) {
        sendBlockDamage(blockLocation, 0.0f, RADIUS);
    }
}
