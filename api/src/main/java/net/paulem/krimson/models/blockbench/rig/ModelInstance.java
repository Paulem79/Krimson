package net.paulem.krimson.models.blockbench.rig;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.paulem.krimson.models.blockbench.anim.AnimationPlayer;
import net.paulem.krimson.models.blockbench.anim.BbAnimation;
import net.paulem.krimson.models.blockbench.model.BbModel;
import net.paulem.krimson.packets.entity.VirtualDisplayEntity;
import net.paulem.krimson.packets.entity.VirtualEntityManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * One posed instance of the model in the world.
 *
 * <p>Each {@link RigPart} gets a packet-only {@link VirtualDisplayEntity} anchored at the
 * rig's base location; all the motion lives in each display's transformation matrix, so
 * moving the whole rig is one teleport rather than fifty. Nothing here is a real Bukkit
 * entity — the rig is visible only to players the {@link VirtualEntityManager} has added
 * as viewers, and disappears with zero server-side entity bookkeeping when it does.
 *
 * <p><b>The transform.</b> For a part in bone {@code B} with baked-geometry centre
 * {@code c}, let
 * <pre>  M = W_B · T(pivot) · Rz·Ry·Rx · T(-pivot)     (Blockbench units)</pre>
 * The item display renders its model centred on the entity, so a baked vertex at model
 * coordinate {@code g} sits at {@code (g - 8)/16} blocks before the transform. Solving
 * for the matrix that lands it at {@code M · (g - 8 + c)} gives:
 * <pre>  linear(T_display) = linear(M),  translation(T_display) = M · c / 16</pre>
 * which is what {@link #partMatrix} builds.
 */
public final class ModelInstance {
    /** How far apart two matrices must be before it is worth sending a packet. */
    private static final float MATRIX_EPSILON = 1.0E-4F;

    /** Identity of the rig, for keying it in the manager. */
    public final UUID id = UUID.randomUUID();

    private final BbModel model;
    private final RigManifest manifest;
    private final BoneSolver solver;
    private final AnimationPlayer player = new AnimationPlayer();

    private final Map<String, VirtualDisplayEntity> displays = new LinkedHashMap<>();
    private final Map<String, Matrix4f> lastSent = new HashMap<>();

    private Location base;
    private float yaw;
    private float scale = 1.0F;
    private final Material carrier;
    private final int interpolationTicks;
    private final Vector3f originOffset;

    private final Matrix4f root = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private final Vector3f center = new Vector3f();

    /** Zero-scale matrix, used to park parts that should not be visible. */
    private static final Matrix4f HIDDEN = new Matrix4f().scale(0.0F);

    public ModelInstance(BbModel model, RigManifest manifest, Location base, float yaw,
                         Material carrier, int interpolationTicks,
                         Vector3f originOffset) {
        this.model = model;
        this.manifest = manifest;
        this.solver = new BoneSolver(model);
        this.base = base.clone();
        this.yaw = yaw;
        this.carrier = carrier;
        this.interpolationTicks = Math.max(1, interpolationTicks);
        this.originOffset = new Vector3f(originOffset);
    }

    public AnimationPlayer player() {
        return player;
    }

    public Location base() {
        return base.clone();
    }

    public int displayCount() {
        return displays.size();
    }

    public float scale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        lastSent.clear();
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        lastSent.clear();
    }

    // ------------------------------------------------------------------- lifecycle

    /** Spawns every part's virtual display, already posed, so nothing pops in untransformed. */
    public void spawn() {
        solveInto();
        for (RigPart part : manifest.parts()) {
            Matrix4f matrix = visible(part) ? partMatrix(part) : HIDDEN;
            Matrix4f initial = new Matrix4f(matrix);

            VirtualDisplayEntity display = new VirtualDisplayEntity(VirtualDisplayEntity.Kind.ITEM, base);
            display.setItemStack(itemFor(part));
            display.setItemDisplayTransform(0); // ItemDisplayContext.NONE
            display.setBillboard(VirtualDisplayEntity.Billboard.FIXED);
            display.setTransformationMatrix(initial);
            display.setInterpolationDuration(interpolationTicks);
            display.setInterpolationDelay(0);
            display.setTeleportDuration(interpolationTicks);
            display.setViewRange(1.0F);
            display.setBrightness(15, 15);

            VirtualEntityManager.getInstance().register(display);
            displays.put(part.id, display);
            lastSent.put(part.id, initial);
        }
    }

    public void remove() {
        for (VirtualDisplayEntity display : displays.values()) {
            VirtualEntityManager.getInstance().unregister(display);
        }
        displays.clear();
        lastSent.clear();
    }

    /** True if the rig has no parts to show, e.g. removed already. */
    public boolean isBroken() {
        return displays.isEmpty();
    }

    public void teleport(Location location) {
        this.base = location.clone();
        for (VirtualDisplayEntity display : displays.values()) {
            display.teleport(location);
        }
    }

    // ------------------------------------------------------------------- per tick

    /**
     * Advances the animation and pushes changed matrices.
     *
     * @param deltaSeconds real time since the previous update
     * @return how many entities were actually updated, for the debug readout
     */
    public int tick(float deltaSeconds) {
        boolean wasAnimating = player.isAnimating();
        player.update(deltaSeconds);
        // One extra pass after the animation settles, so the final frame is not skipped.
        if (!wasAnimating && !player.isAnimating() && !lastSent.isEmpty()) {
            return 0;
        }

        solveInto();
        Set<String> revealed = player.movedBones();
        int updated = 0;

        for (RigPart part : manifest.parts()) {
            VirtualDisplayEntity display = displays.get(part.id);
            if (display == null) {
                continue;
            }
            boolean show = part.visibleByDefault || revealed.contains(part.bone);
            Matrix4f target = show ? partMatrix(part) : HIDDEN;
            Matrix4f previous = lastSent.get(part.id);
            if (previous != null && nearlyEqual(previous, target)) {
                continue;
            }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(interpolationTicks);
            display.setTransformationMatrix(target);
            display.pushMetadata();
            if (previous == null) {
                lastSent.put(part.id, new Matrix4f(target));
            } else {
                previous.set(target);
            }
            updated++;
        }
        return updated;
    }

    private void solveInto() {
        root.identity();
        // Blockbench has this model facing -Z; the extra 180 turns it to face +Z, so a
        // rig yaw of 0 points the same way a player with yaw 0 does.
        root.rotateY((float) Math.toRadians(180.0F - yaw));
        if (scale != 1.0F) {
            root.scale(scale);
        }
        solver.solve(root, player);
    }

    private boolean visible(RigPart part) {
        return part.visibleByDefault || player.movedBones().contains(part.bone);
    }

    /** Builds the display transform for one part. See the class comment for the algebra. */
    private Matrix4f partMatrix(RigPart part) {
        Matrix4f bone = solver.matrixOf(part.bone);
        if (bone == null) {
            return HIDDEN;
        }

        scratch.set(bone);
        if (part.hasRotation()) {
            scratch.translate(part.pivot[0], part.pivot[1], part.pivot[2]);
            BoneSolver.rotateZyx(scratch, part.rotation[0], part.rotation[1],
                    part.rotation[2]);
            scratch.translate(-part.pivot[0], -part.pivot[1], -part.pivot[2]);
        }

        center.set(part.center[0], part.center[1], part.center[2]);
        scratch.transformPosition(center);

        Matrix4f out = new Matrix4f(scratch);
        out.setTranslation(center.x / 16.0F + originOffset.x,
                center.y / 16.0F + originOffset.y,
                center.z / 16.0F + originOffset.z);
        return out;
    }

    private ItemStack itemFor(RigPart part) {
        // Built as a real Bukkit ItemStack first so we can use its item_model API, then
        // converted to the packet representation — the carrier item itself is irrelevant
        // beyond needing to exist, since item_model replaces the whole rendered model.
        org.bukkit.inventory.ItemStack bukkitStack = new org.bukkit.inventory.ItemStack(carrier);
        org.bukkit.inventory.meta.ItemMeta meta = bukkitStack.getItemMeta();
        if (meta != null) {
            meta.setItemModel(part.itemModel);
            bukkitStack.setItemMeta(meta);
        }
        return io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(bukkitStack);
    }

    private static boolean nearlyEqual(Matrix4f a, Matrix4f b) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                if (Math.abs(a.get(column, row) - b.get(column, row)) > MATRIX_EPSILON) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------- controls

    public boolean play(String animationName, float fadeSeconds) {
        BbAnimation animation = model.animation(animationName);
        if (animation == null) {
            return false;
        }
        player.play(animation, fadeSeconds);
        lastSent.clear();
        return true;
    }

    public void stop(float fadeSeconds) {
        player.stop(fadeSeconds);
        lastSent.clear();
    }
}
