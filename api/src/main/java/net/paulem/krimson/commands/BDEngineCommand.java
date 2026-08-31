package net.paulem.krimson.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.paulem.krimson.models.Model;
import net.paulem.krimson.models.Models;
import net.paulem.krimson.models.bdengine.BDEngineModel;
import net.paulem.krimson.models.bdengine.BDEngineModel.ModelInstanceHandle;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** {@code /bdengine} — spawn, animate and clean up BDEngine models. */
public final class BDEngineCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "models", "spawn", "play", "loop", "stop", "remove", "removeall", "list", "info");

    /** Radius, in blocks, used to find the instance a player is standing next to. */
    private static final double NEAREST_RADIUS = 16.0D;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            reply(sender, "Usage: /bdengine <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Seule sous-commande utilisable depuis la console.
        if (sub.equals("models")) {
            List<String> keys = modelKeys();
            if (keys.isEmpty()) {
                reply(sender, "Aucun modèle BDEngine enregistré.");
                return true;
            }
            reply(sender, keys.size() + " modèle(s) BDEngine:\n  " + String.join("\n  ", keys));
            return true;
        }

        if (!(sender instanceof Player player)) {
            reply(sender, "Cette sous-commande doit être exécutée par un joueur.");
            return true;
        }

        switch (sub) {
            case "spawn" -> {
                if (args.length < 2) {
                    reply(sender, "Usage: /bdengine spawn <modèle>");
                    return true;
                }
                BDEngineModel model = requireModel(sender, args[1]);
                if (model == null) {
                    return true;
                }
                ModelInstanceHandle instance = model.spawn(player.getLocation());
                if (instance.displays().isEmpty()) {
                    reply(sender, "Le modèle " + model.getKey() + " n'a produit aucun display.");
                    return true;
                }
                reply(sender, "Modèle " + model.getKey() + " apparu (" + instance.displays().size()
                        + " displays, instance " + shortId(instance.instanceId()) + ").");
            }
            case "play", "loop" -> {
                ModelInstanceHandle instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                String animation = args.length >= 2 ? args[1] : null;
                if (animation != null && !instance.model().getAnimations().containsKey(animation)) {
                    reply(sender, "Animation inconnue: " + animation + ". Essayez /bdengine list");
                    return true;
                }
                boolean loop = sub.equals("loop");
                if (animation == null) {
                    if (loop) {
                        instance.model().playAnimationLoop(player.getWorld(), instance.instanceId());
                    } else {
                        instance.model().playAnimation(player.getWorld(), instance.instanceId());
                    }
                } else if (loop) {
                    instance.model().playAnimationLoop(player.getWorld(), instance.instanceId(), animation);
                } else {
                    instance.model().playAnimation(player.getWorld(), instance.instanceId(), animation);
                }
                reply(sender, (loop ? "Lecture en boucle de " : "Lecture de ")
                        + (animation == null ? "l'animation par défaut" : animation)
                        + " sur l'instance " + shortId(instance.instanceId()) + ".");
            }
            case "stop" -> {
                ModelInstanceHandle instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                BDEngineModel.cancelActiveAnimation(instance.instanceId());
                reply(sender, "Animation arrêtée sur l'instance " + shortId(instance.instanceId()) + ".");
            }
            case "remove" -> {
                ModelInstanceHandle instance = requireNearest(player);
                if (instance == null) {
                    return true;
                }
                BDEngineModel.removeModelInstance(player.getWorld(), instance.instanceId());
                reply(sender, "Instance " + shortId(instance.instanceId()) + " supprimée.");
            }
            case "removeall" -> {
                List<String> ids = new ArrayList<>(instancesInWorld(player.getWorld()).keySet());
                for (String id : ids) {
                    BDEngineModel.removeModelInstance(player.getWorld(), id);
                }
                reply(sender, ids.size() + " instance(s) supprimée(s) dans ce monde.");
            }
            case "list" -> {
                BDEngineModel model;
                if (args.length >= 2) {
                    model = requireModel(sender, args[1]);
                    if (model == null) {
                        return true;
                    }
                } else {
                    ModelInstanceHandle instance = requireNearest(player);
                    if (instance == null) {
                        return true;
                    }
                    model = instance.model();
                }
                if (model.getAnimations().isEmpty()) {
                    reply(sender, model.getKey() + " n'a aucune animation.");
                    return true;
                }
                StringBuilder builder = new StringBuilder(model.getAnimations().size()
                        + " animation(s) pour " + model.getKey() + ":");
                model.getAnimations().forEach((name, keyframes) -> builder.append("\n  ").append(name)
                        .append(" (").append(keyframes.size()).append(" keyframes)"));
                reply(sender, builder.toString());
            }
            case "info" -> {
                Map<String, ModelInstanceHandle> instances = instancesInWorld(player.getWorld());
                ModelInstanceHandle nearest = nearest(player);
                StringBuilder builder = new StringBuilder("instances dans ce monde: " + instances.size());
                if (nearest == null) {
                    builder.append("\nla plus proche: aucune");
                } else {
                    builder.append("\nla plus proche: ").append(shortId(nearest.instanceId()))
                            .append(" — modèle ").append(nearest.model().getKey())
                            .append(", ").append(nearest.displays().size()).append(" displays");
                }
                reply(sender, builder.toString());
            }
            default -> reply(sender, "Sous-commande inconnue. Une de: "
                    + String.join(", ", SUBCOMMANDS));
        }
        return true;
    }

    // --- RECHERCHE DES INSTANCES ---

    /** Toutes les instances actives dont l'emplacement de base est dans ce monde. */
    private static Map<String, ModelInstanceHandle> instancesInWorld(World world) {
        Map<String, ModelInstanceHandle> result = new java.util.LinkedHashMap<>();
        BDEngineModel.activeInstances().forEach((id, instance) -> {
            if (world.equals(instance.location().getWorld())) {
                result.put(id, instance);
            }
        });
        return result;
    }

    private static ModelInstanceHandle nearest(Player player) {
        Location origin = player.getLocation();
        ModelInstanceHandle best = null;
        double bestDistance = NEAREST_RADIUS * NEAREST_RADIUS;

        for (ModelInstanceHandle instance : instancesInWorld(player.getWorld()).values()) {
            double distance = instance.location().distanceSquared(origin);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = instance;
            }
        }
        return best;
    }

    private ModelInstanceHandle requireNearest(Player player) {
        ModelInstanceHandle instance = nearest(player);
        if (instance == null) {
            reply(player, "Aucun modèle à proximité — lancez /bdengine spawn <modèle> d'abord.");
        }
        return instance;
    }

    private static BDEngineModel modelOrNull(String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) return null;
        Model<?, ?, ?> model = Models.REGISTRY.getOrNull(namespacedKey);
        return model instanceof BDEngineModel bdEngine ? bdEngine : null;
    }

    private BDEngineModel requireModel(CommandSender sender, String key) {
        BDEngineModel model = modelOrNull(key);
        if (model == null) {
            reply(sender, "Modèle BDEngine introuvable: " + key + ". Essayez /bdengine models");
        }
        return model;
    }

    private static List<String> modelKeys() {
        List<String> keys = new ArrayList<>();
        for (NamespacedKey key : Models.REGISTRY.keys()) {
            if (Models.REGISTRY.getOrNull(key) instanceof BDEngineModel) {
                keys.add(key.toString());
            }
        }
        return keys;
    }

    private static String shortId(String instanceId) {
        if (instanceId == null) return "?";
        return instanceId.length() <= 8 ? instanceId : instanceId.substring(0, 8);
    }

    private static void reply(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[bdengine] ", NamedTextColor.LIGHT_PURPLE)
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
                return partial(modelKeys(), args[1]);
            }
            if ((sub.equals("play") || sub.equals("loop")) && sender instanceof Player player) {
                ModelInstanceHandle instance = nearest(player);
                if (instance == null) return Collections.emptyList();
                return partial(new ArrayList<>(instance.model().getAnimations().keySet()), args[1]);
            }
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
