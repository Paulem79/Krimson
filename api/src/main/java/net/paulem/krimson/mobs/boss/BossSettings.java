package net.paulem.krimson.mobs.boss;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Optional boss behaviour attached to a {@code CustomMobType}: a boss bar tracking its
 * health, plus health-percentage "phases" that can swap its animation set, buff it, summon
 * adds, etc. A giraffe or a reskinned zombie simply won't have one of these.
 */
public final class BossSettings {
    private final String title;
    private final BarColor color;
    private final BarStyle style;
    private final List<Phase> phases;

    private BossSettings(Builder builder) {
        this.title = builder.title;
        this.color = builder.color;
        this.style = builder.style;
        this.phases = List.copyOf(builder.phases);
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public String title() {
        return title;
    }

    public BarColor color() {
        return color;
    }

    public BarStyle style() {
        return style;
    }

    public List<Phase> phases() {
        return phases;
    }

    /** A health-percentage threshold ([0,1]) and what to do once the boss drops below it. */
    public record Phase(double healthFractionBelow, Consumer<BossController> onEnter) {
    }

    public static final class Builder {
        private final String title;
        private BarColor color = BarColor.RED;
        private BarStyle style = BarStyle.SOLID;
        private final List<Phase> phases = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder color(BarColor color) {
            this.color = color;
            return this;
        }

        public Builder style(BarStyle style) {
            this.style = style;
            return this;
        }

        /** Phases are entered in the order added; add them from highest health to lowest. */
        public Builder phase(double healthFractionBelow, Consumer<BossController> onEnter) {
            this.phases.add(new Phase(healthFractionBelow, onEnter));
            return this;
        }

        public BossSettings build() {
            return new BossSettings(this);
        }
    }
}
