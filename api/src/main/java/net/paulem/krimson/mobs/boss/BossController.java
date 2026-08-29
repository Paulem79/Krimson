package net.paulem.krimson.mobs.boss;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Runtime companion of {@link BossSettings}: owns the actual {@link BossBar}, keeps it in
 * sync with the mob's health every tick, and fires each phase's callback the first time the
 * boss's health fraction drops below that phase's threshold.
 */
public final class BossController {
    private final LivingEntity entity;
    private final BossSettings settings;
    private final BossBar bar;
    private final Set<Integer> firedPhases = new HashSet<>();
    private final Set<Player> viewers = new HashSet<>();

    public BossController(LivingEntity entity, BossSettings settings) {
        this.entity = entity;
        this.settings = settings;
        this.bar = Bukkit.createBossBar(settings.title(), settings.color(), settings.style());
        this.bar.setVisible(true);
    }

    public LivingEntity entity() {
        return entity;
    }

    public BossBar bar() {
        return bar;
    }

    /** Adds a flag such as {@link BarFlag#DARKEN_SKY} or {@link BarFlag#CREATE_FOG}. */
    public BossController flag(BarFlag flag) {
        bar.addFlag(flag);
        return this;
    }

    /** Called once per mob tick by {@code CustomMobInstance}. */
    public void tick() {
        double max = entity.getAttribute(Attribute.MAX_HEALTH) != null
                ? entity.getAttribute(Attribute.MAX_HEALTH).getValue()
                : entity.getHealth();
        double fraction = max <= 0 ? 0 : Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
        bar.setProgress(fraction);

        // Keep the bar shown to nearby players only, like a vanilla boss's tracked-viewer list.
        for (Player nearby : entity.getWorld().getPlayers()) {
            boolean inRange = nearby.getLocation().distanceSquared(entity.getLocation()) <= (64 * 64);
            if (inRange && viewers.add(nearby)) {
                bar.addPlayer(nearby);
            } else if (!inRange && viewers.remove(nearby)) {
                bar.removePlayer(nearby);
            }
        }

        int index = 0;
        for (BossSettings.Phase phase : settings.phases()) {
            if (fraction < phase.healthFractionBelow() && firedPhases.add(index)) {
                phase.onEnter().accept(this);
            }
            index++;
        }
    }

    /** Call when the boss dies or is removed, to clean up the bar for everyone watching. */
    public void dispose() {
        bar.removeAll();
    }
}
