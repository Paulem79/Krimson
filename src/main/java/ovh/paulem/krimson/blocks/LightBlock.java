package ovh.paulem.krimson.blocks;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public class LightBlock extends CustomBlock {
    public LightBlock() {
        super(Material.SLIME_BLOCK, new ItemStack(Material.AMETHYST_BLOCK), 15);
    }

    public LightBlock(ItemDisplay itemDisplay) {
        super(Material.SLIME_BLOCK, new ItemStack(Material.AMETHYST_BLOCK), 15, itemDisplay);
    }
}
