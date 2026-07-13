package net.paulem.krimsontest;

import net.paulem.krimson.KrimsonAPI;
import net.paulem.krimson.common.KrimsonPlugin;
import net.paulem.krimsontest.blocks.PluginBlocks;
import net.paulem.krimsontest.items.PluginItems;

public class TestPlugin extends KrimsonPlugin<TestPlugin> {
    private KrimsonAPI<TestPlugin> api;

    @Override
    public void onEnable() {
        super.onEnable();

        api = new KrimsonAPI<>(this);
        api.init(true);
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
}
