package net.paulem.krimson.inventories;

import net.paulem.krimson.KrimsonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public abstract class TickableHolder implements InventoryHolder {
    private @Nullable BukkitTask tickTask = null;

    public abstract void tick();

    public @Nullable BukkitTask getTickTask() {
        return tickTask;
    }

    public void startTicking() {
        tickTask = Bukkit.getScheduler().runTaskTimerAsynchronously(KrimsonPlugin.getInstance(), this::tick, 1L, 1L);
    }
}
