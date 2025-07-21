package ovh.paulem.krimson.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import ovh.paulem.krimson.inventories.TickableHolder;

public class InventoryListener implements Listener {
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if(event.getInventory().getHolder() instanceof TickableHolder tickableHolder) {
            tickableHolder.startTicking();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof TickableHolder tickableHolder) {
            if (tickableHolder.getTickTask() != null) {
                tickableHolder.getTickTask().cancel();
            }
        }
    }
}
