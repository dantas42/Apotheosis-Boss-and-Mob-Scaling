package dev.apothbossscaling.scaling;

import dev.apothbossscaling.config.ScalingConfig;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

/**
 * Classifica entidades em "boss" ou "mob comum" sem depender de API universal de boss (que não existe).
 * <p>
 * Heurística (Fase 2):
 * <ul>
 * <li>Boss: tipo na lista {@code boss_entities} da config, ou vida base máxima &gt;= limiar configurável.</li>
 * <li>Mob comum: hostil ({@link Monster}) que não é boss.</li>
 * <li>Animais passivos/neutros nunca são escalados.</li>
 * </ul>
 * Detecção por boss bar ({@code ServerBossEvent}) e overrides por entidade entram na Fase 3.
 */
public final class EntityClassifier {

    private EntityClassifier() {}

    public static boolean isBoss(Mob mob) {
        String id = entityId(mob);
        List<String> bossList = ScalingConfig.bossEntities();
        if (bossList.contains(id)) {
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
