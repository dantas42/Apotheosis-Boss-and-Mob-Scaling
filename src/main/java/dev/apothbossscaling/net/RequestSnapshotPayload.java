package dev.apothbossscaling.net;

import dev.apothbossscaling.ApotheosisBossScaling;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Pedido cliente→servidor de um snapshot atualizado (usado ao abrir/atualizar a tela).
 */
public record RequestSnapshotPayload() implements CustomPacketPayload {

    public static final Type<RequestSnapshotPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ApotheosisBossScaling.MODID, "request_snapshot"));

    public static final StreamCodec<ByteBuf, RequestSnapshotPayload> STREAM_CODEC =
        StreamCodec.unit(new RequestSnapshotPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSnapshotPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Network.sendSnapshot(sp);
            }
        });
    }
}
