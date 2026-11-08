package dev.apothbossscaling.tier;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Ponte única de integração com o Apotheosis. Usa somente API pública.
 * <p>
 * O World Tier do Apotheosis é <b>por jogador</b> (attachment {@code Apoth.Attachments.WORLD_TIER}), não global
 * nem por dimensão. Para monstros, o Apotheosis resolve o <b>jogador mais próximo</b> no momento do spawn/join
 * — replicamos exatamente esse critério.
 */
public final class TierResolver {

    private TierResolver() {}

    /**
     * Retorna o World Tier do jogador via API pública do Apotheosis.
     *
     * @see WorldTier#getTier(Player)
     */
    public static WorldTier tierOf(Player player) {
        return WorldTier.getTier(player);
    }

    /**
     * Retorna o nome serializado do tier do jogador (ex.: "pinnacle").
     */
    public static String tierName(Player player) {
        return tierOf(player).getSerializedName();
    }

    /**
     * Resolve o jogador mais próximo do mob (mesmo critério usado pelo Apotheosis em
     * {@code ApothMobEvents.finalizeMobSpawns}).
     *
     * @return o jogador mais próximo, ou {@code null} se não houver nenhum.
     */
    @Nullable
    public static Player nearestPlayer(ServerLevel level, Mob mob) {
        return level.getNearestPlayer(mob, -1.0);
    }
}
