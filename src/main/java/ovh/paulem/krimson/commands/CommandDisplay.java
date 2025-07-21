package ovh.paulem.krimson.commands;

import ovh.paulem.krimson.blocks.LightBlock;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandDisplay implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player player) {
            Location blockLoc = player.getLocation().getBlock().getLocation();
            //ItemStack displayedItem = new ItemStack(Material.ACACIA_PLANKS); //new Skull("https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63").get();

            new LightBlock().spawn(blockLoc);
        }

        return false;
    }
}
