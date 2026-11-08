package dev.apothbossscaling.net;

import dev.apothbossscaling.ApotheosisBossScaling;
import dev.apothbossscaling.config.ScalingConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro e helpers de rede do mod.
 */
@EventBusSubscriber(modid = ApotheosisBossScaling.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Network {

    private Network() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // ".optional()" torna o client opcional: conexões que não têm estes payloads NÃO são negadas,
        // e os packets são descartados graciosamente em clientes sem o mod.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(ConfigSnapshotPayload.TYPE, ConfigSnapshotPayload.STREAM_CODEC, ConfigSnapshotPayload::handle);
        registrar.playToServer(ConfigUpdatePayload.TYPE, ConfigUpdatePayload.STREAM_CODEC, ConfigUpdatePayload::handle);
        registrar.playToServer(RequestSnapshotPayload.TYPE, RequestSnapshotPayload.STREAM_CODEC, RequestSnapshotPayload::handle);
    }

    public static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, ScalingConfig.buildSnapshot(player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendSnapshot(p);
        }
    }
}
