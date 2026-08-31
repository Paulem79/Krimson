package net.paulem.krimson.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.paulem.krimson.mobs.CustomMobInstance;
import net.paulem.krimson.mobs.CustomMobType;
import net.paulem.krimson.mobs.CustomMobs;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** {@code /custommob} — spawn, animate and clean up custom mobs. */
public final class CustomMobCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "types", "spawn", "anim", "remove", "removeall", "list", "info");

    /** Radius, in blocks, used to find the custom mob a player is standing next to. */
    private static final double NEAREST_RADIUS = 16.0D;

    /** Default duration of a manually triggered animation, in seconds. */
    private static final float DEFAULT_ANIM_SECONDS = 1.0F;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            reply(sender, "Usage: /custommob <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Sous-commandes utilisables depuis la console.
        if (sub.equals("types")) {
            List<String> keys = typeKeys();
            if (keys.isEmpty()) {
                reply(sender, "Aucun mob custom enregistré.");
                return true;
            }
            reply(sender, keys.size() + " type(s) de mob custom:\n  " + String.join("\n  ", keys));
            return true;
        }
        if (sub.equals("list") && args.length >= 2) {
            CustomMobType<?> type = requireType(sender, args[1]);
            if (type == null) {
                return true;
            }
            replyAnimations(sender, type);
            return true;
        }

        if (!(sender instanceof Player player)) {
            reply(sender, "Cette sous-commande doit être exécutée par un joueur.");
            return true;
        }

        switch (sub) {
            case "spawn" -> {
                if (args.length < 2) {
                    reply(sender, "Usage: /custommob spawn <type>");
                    return true;
                }
                CustomMobType<?> type = requireType(sender, args[1]);
                if (type == null) {
                    return true;
                }
                CustomMobInstance instance = spawnUnchecked(type, player.getLocation());
                reply(sender, "Mob " + type.getKey() + " apparu (uuid "
                        + shortId(instance) + (type.isBoss() ? ", boss" : "") + ").");
            }
            case "anim" -> {
                if (args.length < 2) {
                    reply(sender, "Usage: /custommob anim <état|animation> [secondes]");
                    return true;
                }
                CustomMobInstance instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                float seconds = DEFAULT_ANIM_SECONDS;
                if (args.length >= 3) {
                    try {
                        seconds = Float.parseFloat(args[2]);
                    } catch (NumberFormatException exception) {
                        reply(sender, "Durée invalide: " + args[2]);
                        return true;
                    }
                }

                String requested = args[1];
                switch (requested.toLowerCase(Locale.ROOT)) {
                    case "attack" -> instance.triggerAttack();
                    case "hurt" -> instance.triggerHurt();
                    case "death" -> instance.triggerDeath();
                    default -> {
                        // Soit un état déclaré (idle, walk, ...), soit un nom d'animation brut du bbmodel.
                        String animation = instance.type().animations()
                                .getOrDefault(requested.toLowerCase(Locale.ROOT), requested);
                        instance.triggerCustomAnimation(animation, seconds);
                    }
                }
                reply(sender, "Animation " + requested + " déclenchée sur " + shortId(instance) + ".");
            }
            case "remove" -> {
                CustomMobInstance instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                CustomMobs.manager().remove(instance.entity(), true);
                reply(sender, "Mob " + shortId(instance) + " supprimé.");
            }
            case "removeall" -> {
                int count = 0;
                for (CustomMobInstance instance : inWorld(player)) {
                    CustomMobs.manager().remove(instance.entity(), true);
                    count++;
                }
                reply(sender, count + " mob(s) custom supprimé(s) dans ce monde.");
            }
            case "list" -> {
                CustomMobInstance instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                replyAnimations(sender, instance.type());
            }
            case "info" -> {
                CustomMobInstance nearest = nearest(player);
                StringBuilder builder = new StringBuilder(
                        "mobs custom dans ce monde: " + inWorld(player).size()
                                + " (total: " + CustomMobs.manager().instances().size() + ")");
                if (nearest == null) {
                    builder.append("\nle plus proche: aucun");
                } else {
                    builder.append("\nle plus proche: ").append(shortId(nearest))
                            .append(" — type ").append(nearest.type().getKey())
                            .append(String.format(", %.1f/%.1f PV", nearest.entity().getHealth(),
                                    nearest.entity().getAttribute(
                                            org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()))
                            .append(", état ").append(nearest.animation().state())
                            .append(nearest.boss() == null ? "" : ", boss");
                }
                reply(sender, builder.toString());
            }
            default -> reply(sender, "Sous-commande inconnue. Une de: "
                    + String.join(", ", SUBCOMMANDS));
        }
        return true;
    }

    // --- RECHERCHE DES INSTANCES ---

    private static List<CustomMobInstance> inWorld(Player player) {
        List<CustomMobInstance> instances = new ArrayList<>();
        for (CustomMobInstance instance : CustomMobs.manager().instances()) {
            if (instance.entity().getWorld().equals(player.getWorld())) {
                instances.add(instance);
            }
        }
        return instances;
    }

    private static CustomMobInstance nearest(Player player) {
        Location origin = player.getLocation();
        CustomMobInstance best = null;
        double bestDistance = NEAREST_RADIUS * NEAREST_RADIUS;

        for (CustomMobInstance instance : inWorld(player)) {
            double distance = instance.entity().getLocation().distanceSquared(origin);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = instance;
            }
        }
        return best;
    }

    private CustomMobInstance requireNearest(Player player) {
        CustomMobInstance instance = nearest(player);
        if (instance == null) {
            reply(player, "Aucun mob custom à proximité — lancez /custommob spawn <type> d'abord.");
        }
        return instance;
    }

    /**
     * Le registre ne retient que des {@code CustomMobType<?>}: le type capturé ne peut pas
     * satisfaire {@code T extends Mob & KrimsonMob<T>} à la compilation, d'où l'appel brut.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CustomMobInstance spawnUnchecked(CustomMobType<?> type, Location location) {
        return CustomMobs.spawn((CustomMobType) type, location);
    }

    private static CustomMobType<?> typeOrNull(String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return null;
        return CustomMobs.REGISTRY.getOrNull(namespacedKey);
    }

    private CustomMobType<?> requireType(CommandSender sender, String key) {
        CustomMobType<?> type = typeOrNull(key);
        if (type == null) {
            reply(sender, "Mob custom introuvable: " + key + ". Essayez /custommob types");
        }
        return type;
    }

    private static List<String> typeKeys() {
        List<String> keys = new ArrayList<>();
        for (NamespacedKey key : CustomMobs.REGISTRY.keys()) {
            keys.add(key.toString());
        }
        return keys;
    }

    private void replyAnimations(CommandSender sender, CustomMobType<?> type) {
        if (type.animations().isEmpty()) {
            reply(sender, type.getKey() + " n'a aucune animation déclarée.");
            return;
        }
        StringBuilder builder = new StringBuilder(
                type.animations().size() + " animation(s) pour " + type.getKey() + ":");
        type.animations().forEach((state, animation) ->
                builder.append("\n  ").append(state).append(" → ").append(animation));
        reply(sender, builder.toString());
    }

    private static String shortId(CustomMobInstance instance) {
        return instance.entity().getUniqueId().toString().substring(0, 8);
    }

    private static void reply(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[mob] ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.GRAY)));
    }

    // --- COMPLÉTION ---

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        if (args.length == 1) {
            return partial(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("spawn") || sub.equals("list")) {
                return partial(typeKeys(), args[1]);
            }
            if (sub.equals("anim") && sender instanceof Player player) {
                CustomMobInstance instance = nearest(player);
                if (instance == null) return Collections.emptyList();
                List<String> states = new ArrayList<>(instance.type().animations().keySet());
                for (String builtin : List.of("attack", "hurt", "death")) {
                    if (!states.contains(builtin)) {
                        states.add(builtin);
                    }
                }
                return partial(states, args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("anim")) {
            return partial(List.of("0.5", "1", "2"), args[2]);
        }
        return Collections.emptyList();
    }

    private static List<String> partial(List<String> options, String typed) {
        List<String> matches = new ArrayList<>();
        String lower = typed.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
