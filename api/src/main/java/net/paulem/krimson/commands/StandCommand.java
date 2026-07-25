package net.paulem.krimson.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.paulem.krimson.KrimsonPlugin;
import net.paulem.krimson.models.Model;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.blockbench.BlockbenchDisplayModel;
import net.paulem.krimson.models.blockbench.RigManager;
import net.paulem.krimson.models.blockbench.anim.BbAnimation;
import net.paulem.krimson.models.blockbench.model.BbModel;
import net.paulem.krimson.models.blockbench.rig.ModelInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/** {@code /stand} — spawn rigs and drive their animations. */
public final class StandCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "spawn", "remove", "removeall", "play", "stop", "pause", "speed", "scale",
            "list", "info");

    private final KrimsonPlugin<?> plugin;

    public StandCommand(KrimsonPlugin<?> plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (args.length == 0) {
            reply(sender, "Usage: /stand <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        Model theWorld = Models.REGISTRY.getOrThrow(new NamespacedKey(plugin, "the_world"));
        if(!(theWorld instanceof BlockbenchDisplayModel blockbench)) return true;

        BbModel model = blockbench.getModel();
        RigManager rigs = blockbench.getRigs();

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Read-only subcommands do not need a player.
        if (sub.equals("list")) {
            StringBuilder builder = new StringBuilder(
                    model.animations.size() + " animations:");
            for (BbAnimation animation : model.animations.values()) {
                builder.append("\n  ").append(animation.name)
                        .append(String.format("  (%.2fs, %s)", animation.length,
                                animation.loopMode.name().toLowerCase(Locale.ROOT)));
            }
            reply(sender, builder.toString());
            return true;
        }
        if (sub.equals("removeall")) {
            int count = rigs.count();
            rigs.removeAll();
            reply(sender, "Removed " + count + " rig(s).");
            return true;
        }

        if (!(sender instanceof Player player)) {
            reply(sender, "That subcommand has to be run by a player.");
            return true;
        }

        switch (sub) {
            case "spawn" -> {
                ModelInstance instance = rigs.spawnFor(player);
                reply(sender, "Spawned with " + instance.displayCount()
                        + " display entities. Try /stand play Idle");
            }
            case "remove" -> {
                ModelInstance instance = rigs.nearest(player);
                if (instance == null) {
                    reply(sender, "No rig nearby.");
                    return true;
                }
                instance.remove();
                reply(sender, "Removed the nearest rig.");
            }
            case "play" -> {
                if (args.length < 2) {
                    reply(sender, "Usage: /stand play <animation>");
                    return true;
                }
                ModelInstance instance = requireNearest(player, rigs);
                if (instance == null) {
                    return true;
                }
                // Animation names can contain spaces, e.g. "Block Grab".
                String name = String.join(" ",
                        java.util.Arrays.copyOfRange(args, 1, args.length));
                if (!instance.play(name, 0.15F)) {
                    reply(sender, "No animation named \"" + name + "\". Try /stand list");
                    return true;
                }
                BbAnimation animation = model.animation(name);
                reply(sender, String.format("Playing %s (%.2fs, %s)", animation.name,
                        animation.length,
                        animation.loopMode.name().toLowerCase(Locale.ROOT)));
            }
            case "stop" -> {
                ModelInstance instance = requireNearest(player, rigs);
                if (instance == null) {
                    return true;
                }
                instance.stop(0.15F);
                reply(sender, "Stopped — back to the rest pose.");
            }
            case "pause" -> {
                ModelInstance instance = requireNearest(player, rigs);
                if (instance == null) {
                    return true;
                }
                instance.player().setPaused(!instance.player().paused());
                reply(sender, instance.player().paused() ? "Paused." : "Resumed.");
            }
            case "speed" -> {
                ModelInstance instance = requireNearest(player, rigs);
                if (instance == null) {
                    return true;
                }
                Float value = parseFloat(args, 1, 0.01F, 10.0F);
                if (value == null) {
                    reply(sender, "Usage: /stand speed <0.01-10>");
                    return true;
                }
                instance.player().setSpeed(value);
                reply(sender, "Speed " + value + "x");
            }
            case "scale" -> {
                ModelInstance instance = requireNearest(player, rigs);
                if (instance == null) {
                    return true;
                }
                Float value = parseFloat(args, 1, 0.05F, 20.0F);
                if (value == null) {
                    reply(sender, "Usage: /stand scale <0.05-20>");
                    return true;
                }
                instance.setScale(value);
                reply(sender, "Scale " + value + "x");
            }
            case "info" -> {
                ModelInstance instance = rigs.nearest(player);
                BbAnimation animation = instance == null ? null
                        : instance.player().current();
                reply(sender, String.format(
                        "rigs=%d  update period=%d tick(s)  entities updated last tick=%d"
                                + "%nnearest: %s",
                        rigs.count(), rigs.periodTicks(), rigs.lastUpdateCount(),
                        instance == null ? "none"
                                : String.format("%d entities, scale %.2f, %s",
                                        instance.displayCount(), instance.scale(),
                                        animation == null ? "rest pose"
                                                : animation.name + " @ "
                                                + String.format("%.2fs",
                                                        instance.player().elapsed()))));
            }
            default -> reply(sender, "Unknown subcommand. One of: "
                    + String.join(", ", SUBCOMMANDS));
        }
        return true;
    }

    private ModelInstance requireNearest(Player player, RigManager rigs) {
        ModelInstance instance = rigs.nearest(player);
        if (instance == null) {
            reply(player, "No rig nearby — run /stand spawn first.");
        }
        return instance;
    }

    private static Float parseFloat(String[] args, int index, float min, float max) {
        if (args.length <= index) {
            return null;
        }
        try {
            float value = Float.parseFloat(args[index]);
            if (value < min || value > max) {
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void reply(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[stand] ", NamedTextColor.AQUA)
                .append(Component.text(message, NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        if (args.length == 1) {
            return partial(SUBCOMMANDS, args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("play")) {
            Model theWorld = Models.REGISTRY.getOrThrow(new NamespacedKey(plugin, "the_world"));
            if(!(theWorld instanceof BlockbenchDisplayModel blockbench)) return Collections.emptyList();
            BbModel model = blockbench.getModel();

            String typed = String.join(" ",
                    java.util.Arrays.copyOfRange(args, 1, args.length));
            List<String> matches = new ArrayList<>();
            for (String name : model.animations.keySet()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) {
                    // Suggest the remaining words so multi-word names complete sensibly.
                    matches.add(name.contains(" ") && args.length > 2
                            ? name.substring(name.indexOf(' ') + 1)
                            : name);
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }

    private static List<String> partial(List<String> options, String typed) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(typed.toLowerCase(Locale.ROOT))) {
                matches.add(option);
            }
        }
        return matches;
    }
}
