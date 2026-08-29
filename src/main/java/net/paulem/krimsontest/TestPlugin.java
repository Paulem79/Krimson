package net.paulem.krimsontest;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.bdengine.BDEngineModel;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;
import net.paulem.krimson.sounds.CustomSound;
import net.paulem.krimsontest.blocks.PluginBlocks;
import net.paulem.krimsontest.items.PluginItems;
import net.paulem.krimsontest.mobs.PluginMobs;
import net.paulem.krimsontest.models.PluginModels;
import net.paulem.krimsontest.sounds.PluginSounds;
import net.paulem.krimsontest.ui.PluginUIs;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TestPlugin extends KrimsonPlugin<TestPlugin> implements Listener {
    private KrimsonAPI<TestPlugin> api;
    private Map<Player, ModelInstance> standsInstances = new HashMap<>();

    @Override
    public void onEnable() {
        super.onEnable();

        api = new KrimsonAPI<>(this);
        api.init(true);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        api.stop();
    }

    @Override
    public void initBlocks() {
        PluginBlocks.init();
    }

    @Override
    public void initItems() {
        PluginItems.init();
    }

    @Override
    public void initModels() {
        PluginModels.init();
    }

    @Override
    public void initMobs() {
        PluginMobs.init();
    }

    @Override
    public void initSounds() {
        PluginSounds.init();
    }

    @Override
    public void initUIs() {
        PluginUIs.init();
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if(!event.isSneaking()) return;

        Player player = event.getPlayer();
        Location location = player.getLocation();

        location.setPitch(0);

        PluginModels.ANIMATED_MODEL.spawn(location);
        /*Location location = getStandLocation(player);

        if(standsInstances.containsKey(player)) {
            ModelInstance instance = standsInstances.get(player);
            instance.remove();
            standsInstances.remove(player);
            return;
        }

        ModelInstance spawnedInstance = PluginModels.THE_WORLD.spawn(location);
        standsInstances.put(player, spawnedInstance);

        PluginSounds.SUMMON_KILLER_QUEEN.play(player, 1, 1);*/
    }


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
        if (!pdc.has(BDEngineModel.INSTANCE_KEY, PersistentDataType.STRING)) return;

        String instanceId = pdc.get(BDEngineModel.INSTANCE_KEY, PersistentDataType.STRING);
        String modelKeyStr = pdc.get(BDEngineModel.MODEL_KEY, PersistentDataType.STRING);

        if (modelKeyStr == null || instanceId == null) return;

        BDEngineModel model = (BDEngineModel) Models.REGISTRY.getOrThrow(NamespacedKey.fromString(modelKeyStr));

        model.playAnimationLoop(display.getWorld(), instanceId);
        PluginSounds.TEST_SOUND.play(player.getLocation(), 1, 1);
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
        if (!pdc.has(BDEngineModel.INSTANCE_KEY, PersistentDataType.STRING)) return;

        String instanceId = pdc.get(BDEngineModel.INSTANCE_KEY, PersistentDataType.STRING);
        if (instanceId == null) return;

        event.setCancelled(true);

        // Use the new method to remove model instance and stop animations/sounds
        BDEngineModel.removeModelInstance(display.getWorld(), instanceId);
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

    /*@EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location standLocation = getStandLocation(player);

        if(standsInstances.containsKey(player)) {
            ModelInstance instance = standsInstances.get(player);
            instance.teleport(standLocation);
        }
    }*/

    /**
     * Calcule la location du Stand à la droite du joueur ET 1 bloc devant lui,
     * orienté dans la même direction que lui.
     */
    /*private Location getStandLocation(Player player) {
        Location playerLoc = player.getLocation();
        float yaw = playerLoc.getYaw();
        float pitch = 0;
        double radians = Math.toRadians(yaw);

        // Décalage à droite du joueur (inchangé, il était bon)
        double rightDistance = 1.2;
        double offsetX = -Math.cos(radians) * rightDistance;
        double offsetZ = -Math.sin(radians) * rightDistance;

        // Décalage 1 bloc devant le joueur (même convention que Bukkit getDirection())
        double forwardDistance = 0.8;
        double forwardX = -Math.sin(radians) * forwardDistance;
        double forwardZ = Math.cos(radians) * forwardDistance;

        Location standLoc = playerLoc.clone().add(offsetX + forwardX, 0, offsetZ + forwardZ);
        standLoc.setYaw(yaw + 180f - 45f);
        standLoc.setPitch(pitch);
        return standLoc;
    }

    private Map<Player, Boolean> justStandAttacked = new HashMap<>();

    @EventHandler
    public void onPlayerRightClick(PrePlayerAttackEntityEvent event) {
        if(justStandAttacked.containsKey(event.getPlayer())) {
            justStandAttacked.remove(event.getPlayer());
            return;
        }
        if(event.isCancelled() || !event.willAttack()) return;

        Player player = event.getPlayer();
        ModelInstance instance = standsInstances.get(player);
        if(instance == null) return;

        event.setCancelled(true);
        justStandAttacked.put(player, true);

        // Random between 1 and 3
        int randSound = ThreadLocalRandom.current().nextInt(1, 4);
        CustomSound mudaSound = switch (randSound) {
            case 1 -> PluginSounds.STAND_THEWORLD_MUDA2;
            case 2 -> PluginSounds.STAND_THEWORLD_MUDA3;
            case 3 -> PluginSounds.THEWORLD_MUDA;
            default -> throw new IllegalStateException("Unexpected value: " + randSound);
        };
        mudaSound.play(player, 1, 1);

        int randAnimation = ThreadLocalRandom.current().nextInt(1, 4);
        String animation = switch (randAnimation) {
            case 1 -> "Combo1";
            case 2 -> "Combo2";
            case 3 -> "Combo3";
            default -> throw new IllegalStateException("Unexpected value: " + randAnimation);
        };
        instance.play(animation, 0.15f);

        player.attack(event.getAttacked());
    }*/

    /**
     * Demo only: right-click with a stick to cycle through spawning the three example
     * custom mobs from {@link PluginMobs} in front of you.
     */
    /*@EventHandler
    public void onRightClickSpawnDemo(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.STICK) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }

        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        spawnLocation.setY(player.getLocation().getY());

        int choice = spawnDemoIndex++ % 3;
        switch (choice) {
            case 0 -> {
                net.paulem.krimson.mobs.CustomMobs.spawn(PluginMobs.GIRAFFE, spawnLocation);
                player.sendMessage("§aSpawned a giraffe.");
            }
            case 1 -> {
                net.paulem.krimson.mobs.CustomMobs.spawn(PluginMobs.WRAITH_ZOMBIE, spawnLocation);
                player.sendMessage("§cSpawned a wraith zombie.");
            }
            default -> {
                net.paulem.krimson.mobs.CustomMobs.spawn(PluginMobs.CINDER_TITAN, spawnLocation);
                player.sendMessage("§4Spawned the Cinder Titan boss.");
            }
        }
    }

    private int spawnDemoIndex = 0;*/
}
