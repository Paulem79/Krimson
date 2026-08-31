package net.paulem.krimson.packets.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A client-side-only entity: never added to a Bukkit world, driven entirely by packets
 * sent to whichever players are currently {@link #viewers()}.
 *
 * <p>Subclasses provide the spawn packet (entity type is fixed at construction) and are
 * responsible for building whatever {@link EntityData} their entity type needs.
 */
public abstract class VirtualEntity {
    protected final int entityId = SpigotReflectionUtil.generateEntityId();
    protected final UUID uuid = UUID.randomUUID();
    protected final EntityType type;
    protected final Set<UUID> viewers = new HashSet<>();

    protected Location location;

    protected VirtualEntity(EntityType type, Location location) {
        this.type = type;
        this.location = location.clone();
    }

    public int entityId() {
        return entityId;
    }

    public UUID uuid() {
        return uuid;
    }

    public Location location() {
        return location.clone();
    }

    public Set<UUID> viewers() {
        return viewers;
    }

    public boolean hasViewer(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    /** Builds the packet(s) needed to make this entity appear for a player, in order. */
    protected abstract List<PacketWrapper<?>> spawnPackets();

    /** Adds a player as a viewer, sending the spawn packets. No-op if already a viewer. */
    public void addViewer(Player player) {
        if (!viewers.add(player.getUniqueId())) {
            return;
        }
        for (PacketWrapper<?> packet : spawnPackets()) {
            send(player, packet);
        }
    }

    /** Removes a player as a viewer, sending a destroy packet. No-op if not a viewer. */
    public void removeViewer(Player player) {
        if (!viewers.remove(player.getUniqueId())) {
            return;
        }
        send(player, new WrapperPlayServerDestroyEntities(entityId));
    }

    /** Drops every viewer without sending a destroy packet (e.g. the player already left). */
    public void dropViewer(UUID playerId) {
        viewers.remove(playerId);
    }

    /** Broadcasts a metadata update to every current viewer. */
    protected void broadcastMetadata(List<EntityData<?>> metadata) {
        if (metadata.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        broadcast(packet);
    }

    public void teleport(Location location) {
        this.location = location.clone();
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                false);
        broadcast(packet);
    }

    protected void broadcast(PacketWrapper<?> packet) {
        for (UUID viewerId : viewers) {
            Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null) {
                send(player, packet);
            }
        }
    }

    protected void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /** Destroys this entity for every current viewer and clears the viewer set. */
    public void remove() {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
        broadcast(packet);
        viewers.clear();
    }
}
