package ovh.paulem.krimson.inventories;

import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InventoryDiff {
    @Setter
    @Nullable
    private ItemStack[] before;
    @Setter
    @Nullable
    private ItemStack[] now;

    public boolean hasChanges() {
        if(before == null || now == null) {
            return false;
        }

        if (before.length != now.length) {
            return true;
        }

        for (int i = 0; i < before.length; i++) {
            // Check if the item at index i is null in both inventories
            ItemStack beforeItem = before[i];
            ItemStack nowItem = now[i];

            if (!Objects.equals(beforeItem, nowItem)) {
                return true;
            }
        }

        return false;
    }
}
