package net.paulem.krimsontest;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.commands.BDEngineCommand;
import net.paulem.krimsontest.blocks.PluginBlocks;
import net.paulem.krimsontest.commands.UICommand;
import net.paulem.krimsontest.items.PluginItems;
import net.paulem.krimsontest.mobs.PluginMobs;
import net.paulem.krimsontest.models.PluginModels;
import net.paulem.krimsontest.sounds.PluginSounds;
import net.paulem.krimsontest.ui.PluginUIs;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;

public class TestPlugin extends KrimsonPlugin<TestPlugin> implements Listener {
    private KrimsonAPI<TestPlugin> api;

    @Override
    public void onEnable() {
        super.onEnable();

        api = new KrimsonAPI<>(this);
        api.init(true);

        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand uiCommand = getInstance().getCommand("ui");
        UICommand uiCommandInstance = new UICommand();
        uiCommand.setExecutor(uiCommandInstance);
        uiCommand.setTabCompleter(uiCommandInstance);
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
}
