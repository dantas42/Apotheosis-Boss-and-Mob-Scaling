package dev.apothbossscaling.event;

import dev.apothbossscaling.net.Network;
import dev.apothbossscaling.scaling.DamageScaler;
import dev.apothbossscaling.scaling.ScalingEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Handlers dos eventos NeoForge usados pelo scaling (todos server-side).
 */
public final class ScalingEventHandler {

    /** Aplica o scaling no spawn (mesma prioridade LOW usada pelo Apotheosis). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isCanceled() || event.isSpawnCancelled()) {
            return;
        }
        ScalingEngine.onEntityJoinOrSpawn(event.getLevel().getLevel(), event.getEntity());
    }

    /** Reaplica (idempotente) para entidades que entram no nível sem passar pelo spawn (chunk load, summon, dimensão). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        ScalingEngine.onEntityJoinOrSpawn(level, mob);
    }

    /** Escala o dano desferido por entidades escaladas (cobre melee, projéteis e magia). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageScaler.onIncomingDamage(event);
    }

    /** Envia o snapshot da config ao jogador ao entrar no servidor. */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Network.sendSnapshot(sp);
        }
    }

    /** Reavalia dinamicamente o scaling por jogadores (a cada ~2s). */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ScalingEngine.onServerTick(event.getServer());
    }
}
