package dev.apothbossscaling.config;

import java.util.List;

/**
 * Presets de dificuldade. Cada preset define as curvas de multiplicadores (vida, dano e armadura,
 * separados) para mobs comuns e bosses. A lógica de scaling é independente: presets apenas preenchem valores.
 */
public final class Presets {

    public record Preset(
        String id,
        String name,
        double[] commonHealth,
        double[] commonDamage,
        double[] commonArmor,
        double[] bossHealth,
        double[] bossDamage,
        double[] bossArmor) {}

    public static final List<Preset> ALL = List.of(
        new Preset("balanced", "Balanced",
            d(1.0, 1.08, 1.16, 1.25, 1.35), d(1.0, 1.05, 1.1, 1.15, 1.2), d(1.0, 1.05, 1.1, 1.15, 1.2),
            d(1.0, 1.1, 1.2, 1.35, 1.5), d(1.0, 1.08, 1.15, 1.25, 1.35), d(1.0, 1.05, 1.12, 1.2, 1.3)),
        new Preset("hard", "Hard",
            d(1.0, 1.1, 1.22, 1.4, 1.6), d(1.0, 1.08, 1.16, 1.28, 1.4), d(1.0, 1.08, 1.16, 1.28, 1.4),
            d(1.0, 1.15, 1.35, 1.6, 1.9), d(1.0, 1.1, 1.25, 1.4, 1.6), d(1.0, 1.1, 1.22, 1.4, 1.6)),
        new Preset("brutal", "Brutal",
            d(1.0, 1.12, 1.3, 1.55, 1.85), d(1.0, 1.1, 1.22, 1.4, 1.6), d(1.0, 1.1, 1.22, 1.4, 1.6),
            d(1.0, 1.2, 1.5, 1.85, 2.3), d(1.0, 1.12, 1.3, 1.55, 1.85), d(1.0, 1.12, 1.3, 1.55, 1.85)),
        new Preset("nightmare", "Nightmare",
            d(1.0, 1.15, 1.4, 1.7, 2.1), d(1.0, 1.12, 1.3, 1.55, 1.85), d(1.0, 1.12, 1.3, 1.55, 1.85),
            d(1.0, 1.25, 1.65, 2.1, 2.7), d(1.0, 1.15, 1.4, 1.7, 2.1), d(1.0, 1.15, 1.4, 1.7, 2.1)));

    private static double[] d(double... v) {
        return v;
    }

    public static Preset byId(String id) {
        for (Preset p : ALL) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private Presets() {}
}
