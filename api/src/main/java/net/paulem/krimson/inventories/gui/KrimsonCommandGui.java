package net.paulem.krimson.inventories.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import net.paulem.krimson.items.CustomItem;
import net.paulem.krimson.items.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KrimsonCommandGui {
    private KrimsonCommandGui() {
        /* This utility class should not be instantiated */
    }

    public static void invoke(Player player) {
        ChestGui gui = new ChestGui(6, "Shop");

        PaginatedPane pages = new PaginatedPane(9, 5);
        pages.populateWithItemStacks(Items.REGISTRY.values()
                .stream()
                .map(CustomItem::getItemStack)
                .toList()
        );
        pages.setOnClick(event -> {
            ItemStack stack = event.getCurrentItem();

            if (stack == null) {
                return;
            }

            Player whoClicked = (Player) event.getWhoClicked();

            whoClicked.getInventory().addItem(stack);

            event.setCancelled(true);
        });

        gui.addPane(Slot.fromXY(0, 0), pages);

        OutlinePane background = new OutlinePane(9, 1);
        background.addItem(new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> event.setCancelled(true)));
        background.setRepeat(true);
        background.setPriority(Pane.Priority.LOWEST);

        gui.addPane(Slot.fromXY(0, 5), background);

        StaticPane navigation = new StaticPane(9, 1);
        navigation.addItem(new GuiItem(new ItemStack(Material.RED_WOOL), event -> {
            if (pages.getPage() > 0) {
                pages.setPage(pages.getPage() - 1);

                gui.update();
            }

            event.setCancelled(true);
        }), 0, 0);

        navigation.addItem(new GuiItem(new ItemStack(Material.GREEN_WOOL), event -> {
            if (pages.getPage() < pages.getPages() - 1) {
                pages.setPage(pages.getPage() + 1);

                gui.update();
            }

            event.setCancelled(true);
        }), 8, 0);

        navigation.addItem(new GuiItem(new ItemStack(Material.BARRIER), event ->
                event.getWhoClicked().closeInventory()), 4, 0);

        gui.addPane(Slot.fromXY(0, 5), navigation);

        gui.show(player);
    }
}
