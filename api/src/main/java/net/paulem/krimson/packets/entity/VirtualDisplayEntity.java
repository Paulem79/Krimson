package net.paulem.krimson.packets.entity;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * A packet-only stand-in for a Bukkit {@code ItemDisplay}/{@code BlockDisplay}: same
 * entity-metadata layout, but nothing is ever spawned into the world.
 *
 * <p>Metadata indices below match the vanilla {@code Display}/{@code ItemDisplay}/
 * {@code BlockDisplay} entity classes as of 1.20.4-1.21.x. Older clients are handled by
 * PacketEvents' protocol translation for the packet types themselves, but a target below
 * 1.19.4 (before display entities existed) simply cannot render one.
 */
public final class VirtualDisplayEntity extends VirtualEntity {
    private static final int IDX_INTERPOLATION_DELAY = 8;
    private static final int IDX_INTERPOLATION_DURATION = 9;
    private static final int IDX_TELEPORT_DURATION = 10;
    private static final int IDX_TRANSLATION = 11;
    private static final int IDX_SCALE = 12;
    private static final int IDX_LEFT_ROTATION = 13;
    private static final int IDX_RIGHT_ROTATION = 14;
    private static final int IDX_BILLBOARD = 15;
    private static final int IDX_BRIGHTNESS_OVERRIDE = 16;
    private static final int IDX_VIEW_RANGE = 17;
    private static final int IDX_SHADOW_RADIUS = 18;
    private static final int IDX_SHADOW_STRENGTH = 19;
    private static final int IDX_WIDTH = 20;
    private static final int IDX_HEIGHT = 21;
    private static final int IDX_GLOW_COLOR_OVERRIDE = 22;
    private static final int IDX_BLOCK_STATE = 23;
    private static final int IDX_ITEM_STACK = 23;
    private static final int IDX_ITEM_DISPLAY_TRANSFORM = 24;

    public enum Kind { ITEM, BLOCK }
    public enum Billboard { FIXED, VERTICAL, HORIZONTAL, CENTER }

    private final Kind kind;

    private Vector3f translation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f(1, 1, 1);
    private Quaternion4f leftRotation = new Quaternion4f(0, 0, 0, 1);
    private Billboard billboard = Billboard.FIXED;
    private int brightnessOverride = -1;
    private float viewRange = 1.0F;
    private int interpolationDuration = 1;
    private int interpolationDelay = 0;
    private int teleportDuration = 1;

    private ItemStack itemStack;
    private int itemDisplayTransform;
    private WrappedBlockState blockState;

    public VirtualDisplayEntity(Kind kind, Location location) {
        super(kind == Kind.ITEM ? EntityTypes.ITEM_DISPLAY : EntityTypes.BLOCK_DISPLAY, location);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setItemDisplayTransform(int transform) {
        this.itemDisplayTransform = transform;
    }

    public void setBlockState(WrappedBlockState blockState) {
        this.blockState = blockState;
    }

    public void setBillboard(Billboard billboard) {
        this.billboard = billboard;
    }

    public void setBrightness(int blockLight, int skyLight) {
        // Packed the same way vanilla packs Display.Brightness: sky in the high bits.
        this.brightnessOverride = (blockLight & 0xF) | ((skyLight & 0xF) << 4);
    }

    public void setViewRange(float viewRange) {
        this.viewRange = viewRange;
    }

    public void setInterpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
    }

    public void setInterpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
    }

    public void setTeleportDuration(int ticks) {
        this.teleportDuration = ticks;
    }

    /** Sets the transformation matrix, mirroring Bukkit's {@code Display#setTransformationMatrix}. */
    public void setTransformationMatrix(Matrix4f matrix) {
        org.joml.Vector3f t = matrix.getTranslation(new org.joml.Vector3f());
        org.joml.Vector3f s = matrix.getScale(new org.joml.Vector3f());
        Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());

        this.translation = new Vector3f(t.x, t.y, t.z);
        this.scale = new Vector3f(s.x, s.y, s.z);
        this.leftRotation = new Quaternion4f(rotation.x, rotation.y, rotation.z, rotation.w);
    }

    @Override
    protected List<PacketWrapper<?>> spawnPackets() {
        List<PacketWrapper<?>> packets = new ArrayList<>(2);
        packets.add(new WrapperPlayServerSpawnEntity(
                entityId, java.util.Optional.of(uuid), type,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(), location.getYaw(), location.getYaw(),
                0, java.util.Optional.empty()));
        packets.add(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata(
                entityId, metadata()));
        return packets;
    }

    /** Re-sends every metadata field for this display (used on spawn, and callable for a full refresh). */
    public List<EntityData<?>> metadata() {
        List<EntityData<?>> data = new ArrayList<>();
        data.add(new EntityData(IDX_INTERPOLATION_DELAY, EntityDataTypes.INT, interpolationDelay));
        data.add(new EntityData(IDX_INTERPOLATION_DURATION, EntityDataTypes.INT, interpolationDuration));
        data.add(new EntityData(IDX_TELEPORT_DURATION, EntityDataTypes.INT, teleportDuration));
        data.add(new EntityData(IDX_TRANSLATION, EntityDataTypes.VECTOR3F, translation));
        data.add(new EntityData(IDX_SCALE, EntityDataTypes.VECTOR3F, scale));
        data.add(new EntityData(IDX_LEFT_ROTATION, EntityDataTypes.QUATERNION, leftRotation));
        data.add(new EntityData(IDX_BILLBOARD, EntityDataTypes.BYTE, (byte) billboard.ordinal()));
        data.add(new EntityData(IDX_BRIGHTNESS_OVERRIDE, EntityDataTypes.INT, brightnessOverride));
        data.add(new EntityData(IDX_VIEW_RANGE, EntityDataTypes.FLOAT, viewRange));

        if (kind == Kind.ITEM) {
            data.add(new EntityData(IDX_ITEM_STACK, EntityDataTypes.ITEMSTACK,
                    itemStack != null ? itemStack : ItemStack.EMPTY));
            data.add(new EntityData(IDX_ITEM_DISPLAY_TRANSFORM, EntityDataTypes.BYTE, (byte) itemDisplayTransform));
        } else {
            data.add(new EntityData(IDX_BLOCK_STATE, EntityDataTypes.BLOCK_STATE,
                    blockState != null ? blockState : WrappedBlockState.getDefaultState(
                            com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.AIR)));
        }

        return data;
    }

    /** Sends only the metadata fields, for an already-spawned entity (matrix/appearance update). */
    public void pushMetadata() {
        broadcastMetadata(metadata());
    }
}
