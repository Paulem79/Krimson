package net.paulem.krimsontest;

import net.paulem.krimson.models.BlockDisplayModel;
import net.paulem.krimson.models.Models;
import net.paulem.krimsontest.sounds.PluginSounds;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.Comparator;

public class ModelInteractionListener implements Listener {

    // CLIC DROIT = JOUER ANIMATION
    @EventHandler
    public void onRightClickDisplay(PlayerInteractEvent event) {
        // Évite le double déclenchement avec la main secondaire
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isOp()) return;

        Display display = getNearestDisplay(player, 0.5);
        if (display == null) return;

        PersistentDataContainer pdc = display.getPersistentDataContainer();
        if (!pdc.has(BlockDisplayModel.INSTANCE_KEY, PersistentDataType.STRING)) return;

        String instanceId = pdc.get(BlockDisplayModel.INSTANCE_KEY, PersistentDataType.STRING);
        String modelKeyStr = pdc.get(BlockDisplayModel.MODEL_KEY, PersistentDataType.STRING);

        if (modelKeyStr == null || instanceId == null) return;

        BlockDisplayModel model = Models.REGISTRY.getOrThrow(NamespacedKey.fromString(modelKeyStr));

        model.playAnimationLoop(display.getWorld(), instanceId);

        player.playSound(player.getLocation(), PluginSounds.TEST_SOUND.getSoundKey(), 1.0f, 1.0f);
    }

    // CLIC GAUCHE = SUPPRIMER LE MODÈLE
    @EventHandler
    public void onLeftClickDisplay(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isOp()) return;

        Display display = getNearestDisplay(player, 0.5);
        if (display == null) return;

        PersistentDataContainer pdc = display.getPersistentDataContainer();
        if (!pdc.has(BlockDisplayModel.INSTANCE_KEY, PersistentDataType.STRING)) return;

        String instanceId = pdc.get(BlockDisplayModel.INSTANCE_KEY, PersistentDataType.STRING);
        if (instanceId == null) return;

        event.setCancelled(true);

        // Supprime toutes les entités partageant le même instance_id
        for (Entity entity : display.getWorld().getEntities()) {
            if (entity instanceof Display d) {
                String otherInstanceId = d.getPersistentDataContainer().get(BlockDisplayModel.INSTANCE_KEY, PersistentDataType.STRING);
                if (instanceId.equals(otherInstanceId)) {
                    d.remove();
                }
            }
        }
    }

    /**
     * Calcule le point visé par le joueur (jusqu'à 5 blocs) et trouve le Display
     * le plus proche dans le rayon indiqué.
     */
    private Display getNearestDisplay(Player player, double radius) {
        Location eye = player.getEyeLocation();
        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), 5.0);

        // Point d'impact sur un bloc ou point dans le vide à 3 blocs devant
        Location targetLocation = (rayTrace != null && rayTrace.getHitPosition() != null)
                ? rayTrace.getHitPosition().toLocation(player.getWorld())
                : eye.clone().add(eye.getDirection().multiply(3.0));

        return player.getWorld().getNearbyEntities(targetLocation, radius, radius, radius).stream()
                .filter(Display.class::isInstance)
                .map(Display.class::cast)
                .min(Comparator.comparingDouble(d -> d.getLocation().distanceSquared(targetLocation)))
                .orElse(null);
    }
}