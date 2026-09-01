package net.paulem.krimson.mobs;

import net.minecraft.world.entity.Mob;

/**
 * Sits in {@code net.minecraft.world.entity} on purpose: {@link Mob#goalSelector} and
 * {@link Mob#targetSelector} are {@code protected}, and Java grants protected access to
 * same-package code even without inheritance. That lets {@code CustomMobManager} clear a
 * mob's vanilla AI on any body spawned through the plain Bukkit/Paper API - {@code
 * org.bukkit.entity.EntityType} plus {@code World#spawn} - without needing an NMS subclass
 * of its own for every vanilla entity Krimson wants to use as a body.
 *
 * <p>This is the only place in the mob system that touches an NMS field directly. It stays
 * safe across Minecraft versions as long as {@link Mob} itself keeps living in this package
 * (true since NMS remapping stabilized on Mojang mappings) - if a future version ever
 * renames {@code goalSelector}/{@code targetSelector}, only this file needs updating.
 */
public final class KrimsonGoalAccess {
    private KrimsonGoalAccess() {
    }

    /** Wipes whatever goals the vanilla constructor registered on {@code mob}. */
    public static void clearVanillaGoals(Mob mob) {
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
    }
}
