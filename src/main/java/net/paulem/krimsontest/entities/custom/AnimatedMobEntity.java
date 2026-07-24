package net.paulem.krimsontest.entities.custom;

import net.paulem.krimson.entities.custom.ModelEntity;
import net.paulem.krimson.entities.custom.ModelEntityProperties;
import net.paulem.krimson.models.Models;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * An animated mob entity that uses a BlockDisplayModel
 */
public class AnimatedMobEntity extends ModelEntity {
    public AnimatedMobEntity(NamespacedKey key) {
        super(key, new NamespacedKey("krimson", "reading")); // Use the reading model
    }

    @Override
    public AnimatedMobEntity copyOf() {
        AnimatedMobEntity copy = new AnimatedMobEntity(this.getKey());
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        // Customize the entity
        if (entity != null) {
            entity.setCustomName("§bAnimated Mob");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(40.0);
            entity.setHealth(40.0);
        }
    }

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Play animation when entity moves
        if (entity != null && entity.getVelocity().length() > 0.1) {
            if (Math.random() < 0.1) { // 10% chance per tick to play animation when moving
                playAnimation("default");
            }
        }
    }

    @Override
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.getPlayer().sendMessage("§aYou interacted with an Animated Mob!");

        // Play animation when interacted with
        playAnimation("default");

        event.setCancelled(true);
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            if (damageByEntityEvent.getDamager() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) damageByEntityEvent.getDamager();
                player.sendMessage("§cYou damaged the Animated Mob!");

                // Play animation when damaged
                playAnimation("default");
            }
        }
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.setDroppedExp(20);
        event.getDrops().clear();

        // Play death animation if available
        if (getModel() != null && getModel().getAvailableAnimations().contains("death")) {
            playAnimation("death");
        }

        entity.getWorld().createExplosion(entity.getLocation(), 2.0f, false, false);
    }

    @Override
    protected AnimatedMobEntityProperties createProperties(LivingEntity entity) {
        return new AnimatedMobEntityProperties(entity, this);
    }
}