package net.paulem.krimson.mobs.listeners;

import net.paulem.krimson.mobs.CustomMobInstance;
import net.paulem.krimson.mobs.CustomMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Drives the animation controller from real gameplay events, and keeps a custom mob's
 * targeting sane. Using Bukkit events (rather than overriding version-sensitive NMS
 * {@code hurt}/{@code die} method names) for the animation triggers keeps this half of the
 * system portable across Paper releases.
 */
public final class CustomMobListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        CustomMobInstance instance = CustomMobs.manager().instanceOf(living);
        if (instance != null) {
            instance.triggerHurt();
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        CustomMobInstance instance = CustomMobs.manager().instanceOf(event.getEntity());
        if (instance == null) {
            return;
        }
        instance.triggerDeath();

        float delaySeconds = instance.type().despawnAfterDeathSeconds();
        long delayTicks = Math.max(1L, Math.round(delaySeconds * 20.0F));
        event.getEntity().getServer().getScheduler().runTaskLater(
                net.paulem.krimson.KrimsonPlugin.getInstance(),
                () -> CustomMobs.manager().remove(event.getEntity(), false),
                delayTicks);
    }

    /**
     * A custom mob's target selection is whatever {@code CustomMobType#ai} wired up via the
     * real {@code targetSelector} - this just prevents unrelated vanilla systems (e.g. a
     * zombie-reinforcement call from a nearby real zombie) from overriding that choice.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (CustomMobs.manager().isCustomMob(event.getEntity())
                && event.getReason() == EntityTargetEvent.TargetReason.REINFORCEMENT_TARGET) {
            event.setCancelled(true);
        }
    }
}
