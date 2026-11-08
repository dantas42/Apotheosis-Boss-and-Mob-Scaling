package dev.apothbossscaling.net;

import dev.apothbossscaling.ApotheosisBossScaling;
import dev.apothbossscaling.config.ScalingConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Pedido cliente→servidor de atualização de config (um campo por vez).
 */
public record ConfigUpdatePayload(String field, String value) implements CustomPacketPayload {

    public static final Type<ConfigUpdatePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ApotheosisBossScaling.MODID, "config_update"));

    public static final StreamCodec<ByteBuf, ConfigUpdatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ConfigUpdatePayload::field,
        ByteBufCodecs.STRING_UTF8, ConfigUpdatePayload::value,
        ConfigUpdatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Servidor: valida permissão, aplica e re-sincroniza o snapshot a todos. */
    public static void handle(ConfigUpdatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                if (ScalingConfig.applyUpdate(payload.field(), payload.value())) {
                    Network.syncToAll(sp.getServer());
                }
            }
        });
    }
}
