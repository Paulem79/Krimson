package net.paulem.krimsontest;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimsontest.blocks.PluginBlocks;
import net.paulem.krimsontest.items.PluginItems;
import net.paulem.krimsontest.models.PluginModels;
import net.paulem.krimsontest.sounds.PluginSounds;
import net.paulem.krimsontest.ui.PluginUIs;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class TestPlugin extends KrimsonPlugin<TestPlugin> implements Listener {
    private KrimsonAPI<TestPlugin> api;

    @Override
    public void onEnable() {
        super.onEnable();

        api = new KrimsonAPI<>(this);
        api.init(true);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ModelInteractionListener(), this);
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

        getLogger().info("Sneak!");
        Location location = event.getPlayer().getLocation();
        location.setPitch(0);
        PluginModels.READING.spawn(location);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PluginUIs.MANA_BAR.display(player);
        updateManaBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PluginUIs.MANA_BAR.hide(event.getPlayer());
    }

    public void updateManaBar(Player player) {
        // Get player's mana (from your mana system)
        int currentMana = 20;
        int maxMana = 30;

        // Update the mana bar
        PluginUIs.MANA_BAR.setProgress((double) currentMana / maxMana);
        PluginUIs.MANA_BAR.updateText(String.format("✎ %d/%d Mana", currentMana, maxMana));
    }
}
