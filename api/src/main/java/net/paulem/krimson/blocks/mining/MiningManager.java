package net.paulem.krimson.blocks.mining;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.blocks.custom.CustomBlock;
import net.paulem.krimson.sounds.CustomSound;
import net.paulem.krimson.sounds.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Drives the server side mining of custom blocks: one {@link MiningSession} per player, ticked every tick.
 *
 * <p>The client is frozen with a hidden mining fatigue so it never completes the vanilla break of the
 * carrier block; the whole progression, the cracking animation and the actual break are decided here.</p>
 */
public class MiningManager {
    /** Amplifier of the injected fatigue: high enough that the client's own progress never completes. */
    private static final int FREEZE_AMPLIFIER = 255;
    /** Squared distance past which a session is dropped, slightly above the vanilla reach. */
    private static final double MAX_DISTANCE_SQUARED = 8 * 8;
    /** How often the digging sound is played while mining, in ticks. */
    private static final int DIG_SOUND_PERIOD = 4;

    private static MiningManager instance;

    public static MiningManager getInstance() {
        if (instance == null) {
            instance = new MiningManager();
        }

        return instance;
    }

    private final Map<UUID, MiningSession> sessions = new HashMap<>();
    @Nullable
    private MyScheduledTask task;

    /**
     * Starts the ticking task. Called once from {@link KrimsonAPI#init(boolean)}.
     */
    public void start() {
        if (task != null) return;

        task = KrimsonPlugin.getScheduler().runTaskTimer(this::tick, 1, 1);
    }

    /**
     * Cancels the ticking task and cleanly ends every running session.
     */
    public void stop() {
        for (UUID uuid : Set.copyOf(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                end(player);
            }
        }
        sessions.clear();

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Nullable
    public MiningSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Begins mining {@code block} for {@code player}, freezing their client with a hidden mining fatigue.
     * Re-entrant: starting again on the block already being mined keeps the existing progress.
     */
    public void start(Player player, Block block, CustomBlock customBlock, MiningProperties properties) {
        MiningSession existing = getSession(player);
        if (existing != null) {
            if (existing.getBlock().equals(block)) return;

            end(player);
        }

        PotionEffect ownFatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        int amplifier = ownFatigue == null ? -1 : ownFatigue.getAmplifier();
        int duration = ownFatigue == null ? 0 : ownFatigue.getDuration();

        MiningSession session = new MiningSession(
                block, customBlock.getKey(), properties, customBlock.getBlockMaterial(), amplifier, duration
        );
        session.setTool(player.getInventory().getItemInMainHand().clone());
        session.setDamagePerTick(MiningSpeedCalculator.damagePerTick(
                player, properties, session.getCarrierMaterial(), amplifier
        ));

        sessions.put(player.getUniqueId(), session);
        freeze(player);
    }

    /**
     * Ends the player's session, clearing the cracking animation and restoring their own mining fatigue.
     */
    public void end(Player player) {
        MiningSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        BreakingStageManager.resetBlockDamage(session.getBlock().getLocation());
        unfreeze(player, session);
    }

    private void tick() {
        Iterator<Map.Entry<UUID, MiningSession>> iterator = sessions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, MiningSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            MiningSession session = entry.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            if (!isStillValid(player, session)) {
                iterator.remove();
                BreakingStageManager.resetBlockDamage(session.getBlock().getLocation());
                unfreeze(player, session);
                continue;
            }

            // A tool swap changes the mining speed mid-session, exactly like vanilla.
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!held.isSimilar(session.getTool())) {
                session.setTool(held.clone());
                session.setDamagePerTick(MiningSpeedCalculator.damagePerTick(
                        player, session.getProperties(), session.getCarrierMaterial(), session.getOwnFatigueAmplifier()
                ));
            }

            session.setProgress(session.getProgress() + session.getDamagePerTick());
            session.setTicks(session.getTicks() + 1);

            if (session.getProgress() >= 1f) {
                iterator.remove();
                unfreeze(player, session);
                complete(player, session);
                continue;
            }

            BreakingStageManager.sendBlockDamage(session.getBlock().getLocation(), session.getProgress());

            if (session.getTicks() % DIG_SOUND_PERIOD == 0) {
                playDigSound(session);
            }
        }
    }

    private boolean isStillValid(Player player, MiningSession session) {
        Block block = session.getBlock();

        if (!KrimsonAPI.isCustomBlockFromWatcher(block)) return false;

        CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(block);
        if (customBlock == null || !customBlock.getKey().equals(session.getBlockKey())) return false;

        if (!player.getWorld().equals(block.getWorld())) return false;

        return player.getEyeLocation().distanceSquared(centerOf(block)) <= MAX_DISTANCE_SQUARED;
    }

    /**
     * Finishes the mining: fires a {@link BlockBreakEvent} so the existing listeners get their say, then
     * breaks the custom block, dropping only when the held tool may harvest it.
     */
    public void complete(Player player, MiningSession session) {
        Block block = session.getBlock();
        Location center = centerOf(block);

        CustomBlock customBlock = KrimsonAPI.customBlocks.getBlockAt(block);
        if (customBlock == null) return;

        BlockBreakEvent event = new BlockBreakEvent(block, player);
        event.setDropItems(MiningSpeedCalculator.canHarvest(
                player.getInventory().getItemInMainHand(), session.getProperties()
        ));
        Bukkit.getPluginManager().callEvent(event);

        BreakingStageManager.resetBlockDamage(block.getLocation());

        if (event.isCancelled()) return;

        playBreakSound(session);
        block.getWorld().spawnParticle(
                Particle.BLOCK, center, 40, .3, .3, .3, .1, session.getCarrierMaterial().createBlockData()
        );

        if (event.isDropItems()) {
            // Handles the removal, the drop and the tool damage.
            customBlock.onPlayerBreak(event);
        } else {
            // Wrong tool: the block is still broken, but yields nothing.
            customBlock.remove();
            player.damageItemStack(player.getInventory().getItemInMainHand(), 1);
        }
    }

    private void freeze(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE, PotionEffect.INFINITE_DURATION, FREEZE_AMPLIFIER,
                false, false, false
        ));
    }

    private void unfreeze(Player player, MiningSession session) {
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        if (session.hasOwnFatigue()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, session.getOwnFatigueDuration(), session.getOwnFatigueAmplifier()
            ));
        }
    }

    private void playDigSound(MiningSession session) {
        Location center = centerOf(session.getBlock());
        NamespacedKey custom = session.getProperties().digSound();

        if (custom != null) {
            playCustomOrVanilla(center, custom, .25f, .5f);
            return;
        }

        Sound fallback = session.getCarrierMaterial().createBlockData().getSoundGroup().getHitSound();
        center.getWorld().playSound(center, fallback, SoundCategory.BLOCKS, .25f, .5f);
    }

    private void playBreakSound(MiningSession session) {
        Location center = centerOf(session.getBlock());
        NamespacedKey custom = session.getProperties().breakSound();

        if (custom != null) {
            playCustomOrVanilla(center, custom, 1f, 1f);
            return;
        }

        Sound fallback = session.getCarrierMaterial().createBlockData().getSoundGroup().getBreakSound();
        center.getWorld().playSound(center, fallback, SoundCategory.BLOCKS, 1f, 1f);
    }

    /**
     * Plays a registered {@link CustomSound} when the key names one, otherwise treats the key as a plain
     * (vanilla or resource pack) sound event.
     */
    private void playCustomOrVanilla(Location location, NamespacedKey key, float volume, float pitch) {
        Optional<CustomSound> sound = Sounds.REGISTRY.get(key);

        if (sound.isPresent()) {
            sound.get().play(location, volume, pitch);
            return;
        }

        location.getWorld().playSound(location, key.toString(), SoundCategory.BLOCKS, volume, pitch);
    }

    private static Location centerOf(Block block) {
        return block.getLocation().add(.5, .5, .5);
    }
}
