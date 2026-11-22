package dev.apothbossscaling.scaling;

import dev.apothbossscaling.attachment.ScalingAttachments;
import dev.apothbossscaling.config.ScalingConfig;
import dev.apothbossscaling.tier.TierResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Orquestra o scaling: resolve o tier, classifica a entidade e aplica os multiplicadores.
 * <p>
 * O multiplicador final é {@code tier * jogadoresPróximos^expoente} (estilo ScalingMobs), aplicado
 * a vida e dano. A reavaliação dinâmica ajusta conforme jogadores se aproximam/afastam.
 */
public final class ScalingEngine {

    private ScalingEngine() {}

    /**
     * Chamado no spawn e no join da entidade.
     */
    public static void onEntityJoinOrSpawn(ServerLevel level, Mob mob) {
        if (!ScalingConfig.isEnabled()) {
            return;
        }
        if (mob.level().isClientSide()) {
            return;
        }

        String stored = mob.getData(ScalingAttachments.SCALED_TIER.get());
        if (!stored.isEmpty()) {
            reapply(mob, stored);
            return;
        }

        Player nearest = TierResolver.nearestPlayer(level, mob);
        if (nearest == null) {
            return; // sem contexto de jogador, não escala
        }
        apply(mob, TierResolver.tierName(nearest));
    }

    private static void apply(Mob mob, String tierName) {
        if (!EntityClassifier.shouldScale(mob)) {
            return;
        }

        // Classifica como boss (com vida base, ainda não escalada) e guarda para uso posterior.
        boolean boss = EntityClassifier.isBoss(mob);
        mob.setData(ScalingAttachments.IS_BOSS.get(), boss);

        double playerMult = playerMultiplier(mob);
        double modMult = modMultiplier(mob);
        double healthMult = tierHealth(tierName, boss) * playerMult * modMult;
        double damageMult = tierDamage(tierName, boss) * playerMult * modMult;
        double armorMult = tierArmor(tierName, boss) * playerMult * modMult;

        float healthPct = mob.getHealth() / mob.getMaxHealth();
        if (ScalingConfig.scaleMaxHealth()) {
            AttributeScaler.applyMaxHealth(mob, healthMult);
        }
        if (ScalingConfig.scaleArmor()) {
            AttributeScaler.applyArmor(mob, armorMult, boss ? ScalingConfig.bossArmorCap() : ScalingConfig.mobArmorCap());
        }
        mob.setData(ScalingAttachments.SCALED_TIER.get(), tierName);
        mob.setData(ScalingAttachments.DAMAGE_MULTIPLIER.get(), (float) damageMult);
        mob.setHealth(healthPct * mob.getMaxHealth());

        ScalingConfig.debug("Escalado {} para tier {} (vida x{}, dano x{})", mob.getName().getString(), tierName, healthMult, damageMult);
    }

    private static void reapply(Mob mob, String tierName) {
        boolean boss = mob.getData(ScalingAttachments.IS_BOSS.get());
        double playerMult = playerMultiplier(mob);
        double modMult = modMultiplier(mob);
        double healthMult = tierHealth(tierName, boss) * playerMult * modMult;
        double damageMult = tierDamage(tierName, boss) * playerMult * modMult;
        double armorMult = tierArmor(tierName, boss) * playerMult * modMult;

        float healthPct = mob.getHealth() / mob.getMaxHealth();
        if (ScalingConfig.scaleMaxHealth()) {
            AttributeScaler.applyMaxHealth(mob, healthMult);
        }
        if (ScalingConfig.scaleArmor()) {
            AttributeScaler.applyArmor(mob, armorMult, boss ? ScalingConfig.bossArmorCap() : ScalingConfig.mobArmorCap());
        }
        mob.setData(ScalingAttachments.DAMAGE_MULTIPLIER.get(), (float) damageMult);
        mob.setHealth(healthPct * mob.getMaxHealth());
    }

    /**
     * Reavalia o scaling de uma entidade já escalada. Reaplica os modificadores de forma idempotente
     * (auto-cura: se uma fase/transformação remover atributos, eles voltam), e preserva a fração de vida
     * quando o multiplicador por jogadores muda.
     */
    public static void recomputePlayerScaling(Mob mob) {
        String tier = mob.getData(ScalingAttachments.SCALED_TIER.get());
        if (tier.isEmpty()) {
            return;
        }

        boolean boss = mob.getData(ScalingAttachments.IS_BOSS.get());
        double playerMult = playerMultiplier(mob);
        double modMult = modMultiplier(mob);
        double damageMult = tierDamage(tier, boss) * playerMult * modMult;
        double healthMult = tierHealth(tier, boss) * playerMult * modMult;
        double armorMult = tierArmor(tier, boss) * playerMult * modMult;

        float current = mob.getData(ScalingAttachments.DAMAGE_MULTIPLIER.get());
        boolean changed = Math.abs(damageMult - current) >= 1e-4;

        float healthPct = mob.getHealth() / mob.getMaxHealth();
        if (ScalingConfig.scaleMaxHealth()) {
            AttributeScaler.applyMaxHealth(mob, healthMult);
        }
        if (ScalingConfig.scaleArmor()) {
            AttributeScaler.applyArmor(mob, armorMult, boss ? ScalingConfig.bossArmorCap() : ScalingConfig.mobArmorCap());
        }

        if (changed) {
            mob.setData(ScalingAttachments.DAMAGE_MULTIPLIER.get(), (float) damageMult);
            mob.setHealth(healthPct * mob.getMaxHealth());
            ScalingConfig.debug("Reescalado {} por jogadores próximos (vida x{}, dano x{}, armadura x{})", mob.getName().getString(), healthMult, damageMult, armorMult);
        }
    }

    /** Tick global do servidor para reavaliação dinâmica (chamado a cada ~2s). */
    public static void onServerTick(MinecraftServer server) {
        if (!ScalingConfig.playerScalingEnabled() || !ScalingConfig.playerScalingDynamic()) {
            return;
        }
        if (server.getTickCount() % 40 != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof Mob mob && mob.hasData(ScalingAttachments.SCALED_TIER.get())) {
                    recomputePlayerScaling(mob);
                }
            }
        }
    }

    private static double tierHealth(String tier, boolean boss) {
        return boss ? ScalingConfig.bossMaxHealthMultiplier(tier) : ScalingConfig.maxHealthMultiplier(tier);
    }

    private static double tierDamage(String tier, boolean boss) {
        return boss ? ScalingConfig.bossAttackDamageMultiplier(tier) : ScalingConfig.attackDamageMultiplier(tier);
    }

    private static double tierArmor(String tier, boolean boss) {
        return boss ? ScalingConfig.bossArmorMultiplier(tier) : ScalingConfig.armorMultiplier(tier);
    }

    /** Multiplicador do mod (vanilla vs outros mods vs overrides por mod). Bosses do Apotheosis têm fator próprio. */
    private static double modMultiplier(Mob mob) {
        if (isApotheosisBoss(mob)) {
            return ScalingConfig.modMultApotheosis();
        }
        return ScalingConfig.modMultiplier(EntityClassifier.entityId(mob));
    }

    /** Detecta bosses do Apotheosis (Invaders/Elites) pelas tags persistentes. */
    private static boolean isApotheosisBoss(Mob mob) {
        net.minecraft.nbt.CompoundTag data = mob.getPersistentData();
        return data.contains("apoth.boss") || data.contains("apoth.miniboss");
    }

    /**
     * Multiplicador por jogadores próximos: {@code jogadoresPróximos ^ expoente} (raiz quadrada por padrão).
     * Aplicado <b>apenas a bosses</b> (mobs comuns não são afetados por jogadores próximos).
     */
    private static double playerMultiplier(Mob mob) {
        if (!ScalingConfig.playerScalingEnabled()) {
            return 1.0;
        }
        if (!mob.getData(ScalingAttachments.IS_BOSS.get())) {
            return 1.0;
        }
        double radius = ScalingConfig.playerScalingRadius();
        AABB box = mob.getBoundingBox().inflate(radius);
        int nearby = 0;
        for (Player p : mob.level().players()) {
            if (!p.isSpectator() && p.isAlive() && box.contains(p.getX(), p.getY(), p.getZ())) {
                nearby++;
            }
        }
        return Math.pow(Math.max(1, nearby), ScalingConfig.playerScalingExponent());
    }
}
