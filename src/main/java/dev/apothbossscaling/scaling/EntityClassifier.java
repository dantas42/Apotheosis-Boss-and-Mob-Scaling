package dev.apothbossscaling.scaling;

import dev.apothbossscaling.config.ScalingConfig;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

/**
 * Classifica entidades em "boss" ou "mob comum" sem depender de API universal de boss (que não existe).
 * <p>
 * Heurística:
 * <ul>
 * <li>Boss: tipo na lista {@code boss_entities}, ou um boss vanilla conhecido, ou vida base &gt;= limiar.</li>
 * <li>Mob comum: hostil ({@link Monster}) que não é boss.</li>
 * <li>Animais passivos/neutros nunca são escalados.</li>
 * </ul>
 */
public final class EntityClassifier {

    /** Bosses vanilla sempre tratados como boss (não há API de boss bar; estes são os canônicos). */
    private static final Set<String> VANILLA_BOSSES = Set.of(
        "minecraft:wither", "minecraft:ender_dragon", "minecraft:warden", "minecraft:elder_guardian");

    private EntityClassifier() {}

    public static boolean isBoss(Mob mob) {
        String id = entityId(mob);
        List<String> bossList = ScalingConfig.bossEntities();
        if (bossList.contains(id)) {
            return true;
        }
        if (VANILLA_BOSSES.contains(id)) {
            return true;
        }
        return mob.getMaxHealth() >= ScalingConfig.bossHealthThreshold();
    }

    /**
     * Decide se a entidade deve ser escalada, respeitando exclusões e os toggles boss/mob comum.
     */
    public static boolean shouldScale(Mob mob) {
        if (ScalingConfig.excludedEntities().contains(entityId(mob))) {
            return false;
        }
        if (isBoss(mob)) {
            return ScalingConfig.scaleBosses();
        }
        return mob instanceof Monster && ScalingConfig.scaleCommonMobs();
    }

    public static String entityId(Mob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key == null ? "" : key.toString();
    }
}
