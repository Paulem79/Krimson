package net.paulem.krimsontest.entities.custom;

import net.paulem.krimson.entities.custom.CustomEntity;
import net.paulem.krimson.entities.custom.CustomEntityProperties;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * A simple test mob entity
 */
public class TestMobEntity extends CustomEntity {
    public TestMobEntity(NamespacedKey key) {
        super(key);
    }

    @Override
    public TestMobEntity copyOf() {
        TestMobEntity copy = new TestMobEntity(this.getKey());
        copy.registryReference = false;
        return copy;
    }

    @Override
    protected void initializeEntity() {
        super.initializeEntity();

        // Customize the entity
        if (entity != null) {
            entity.setCustomName("§6Test Mob");
            entity.setCustomNameVisible(true);
            entity.setMaxHealth(30.0);
            entity.setHealth(30.0);
        }
    }

    @Override
    protected void updateEntityState() {
        super.updateEntityState();

        // Add some simple AI - wander around
        if (entity != null && entity.getLocation().getWorld() != null) {
            // Simple wandering behavior
            if (Math.random() < 0.01) { // 1% chance per tick to change direction
                double angle = Math.random() * Math.PI * 2;
                double speed = 0.1;
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(
                    Math.cos(angle) * speed,
                    0,
                    Math.sin(angle) * speed
                )));
            }
        }
    }

    @Override
    public void onInteract(PlayerInteractAtEntityEvent event) {
        event.getPlayer().sendMessage("§aYou interacted with a Test Mob!");
        event.setCancelled(true);
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            if (damageByEntityEvent.getDamager() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) damageByEntityEvent.getDamager();
                player.sendMessage("§cYou damaged the Test Mob!");
            }
        }
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.setDroppedExp(10);
        event.getDrops().clear(); // No drops for now
        entity.getWorld().strikeLightningEffect(entity.getLocation());
    }

    @Override
    protected TestMobEntityProperties createProperties(LivingEntity entity) {
        return new TestMobEntityProperties(entity, this);
    }
}