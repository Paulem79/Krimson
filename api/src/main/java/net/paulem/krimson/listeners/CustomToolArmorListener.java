package net.paulem.krimson.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import net.paulem.krimson.constants.Keys;
import net.paulem.krimson.items.CustomArmorItem;
import net.paulem.krimson.items.CustomItem;
import net.paulem.krimson.items.CustomToolItem;
import net.paulem.krimson.items.Items;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

/**
 * Dispatches interactions on {@link CustomToolItem}s and {@link CustomArmorItem}s to the callbacks the
 * developer registered them with. The vanilla behaviour driven by the data components (attribute modifiers,
 * {@code minecraft:tool} rules, {@code minecraft:equippable}) always applies regardless of these callbacks.
 */
public class CustomToolArmorListener implements Listener {
    private static @Nullable CustomItem resolve(@Nullable ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String identifier = pdc.get(Keys.IDENTIFIER.key(), Keys.IDENTIFIER.type());
        if (identifier == null) {
            return null;
        }

        NamespacedKey key = NamespacedKey.fromString(identifier);
        if (key == null) {
            return null;
        }

        return Items.REGISTRY.get(key).orElse(null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot slot = event.getHand();
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) {
            return;
        }

        if (!(resolve(event.getItem()) instanceof CustomToolItem toolItem)) {
            return;
        }

        if (toolItem.getOnInteract() != null) {
            toolItem.getOnInteract().accept(toolItem, event);
        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (!(resolve(inHand) instanceof CustomToolItem toolItem)) {
            return;
        }

        if (toolItem.getOnBreakBlock() != null) {
            toolItem.getOnBreakBlock().accept(toolItem, event);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        ItemStack inHand = attacker.getInventory().getItemInMainHand();

        if (resolve(inHand) instanceof CustomToolItem toolItem && toolItem.getOnAttack() != null) {
            toolItem.getOnAttack().accept(toolItem, event);
        }

        if (event.getEntity() instanceof LivingEntity victim) {
            dispatchArmorDamage(victim, event);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // EntityDamageByEntityEvent already covers attacker-inflicted damage; this handles every other cause
        // (fall, fire, potion, environmental, ...) so armor callbacks fire for any source of damage.
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity victim) {
            dispatchArmorDamage(victim, event);
        }
    }

    private void dispatchArmorDamage(LivingEntity victim, EntityDamageEvent event) {
        for (ItemStack armorPiece : victim.getEquipment() == null ? new ItemStack[0] : victim.getEquipment().getArmorContents()) {
            if (resolve(armorPiece) instanceof CustomArmorItem armorItem && armorItem.getOnDamaged() != null) {
                armorItem.getOnDamaged().accept(armorItem, event);
            }
        }
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();

        if (resolve(event.getOldItem()) instanceof CustomArmorItem oldArmor && oldArmor.getOnUnequip() != null) {
            oldArmor.getOnUnequip().accept(oldArmor, player);
        }

        if (resolve(event.getNewItem()) instanceof CustomArmorItem newArmor && newArmor.getOnEquip() != null) {
            newArmor.getOnEquip().accept(newArmor, player);
        }
    }
}
