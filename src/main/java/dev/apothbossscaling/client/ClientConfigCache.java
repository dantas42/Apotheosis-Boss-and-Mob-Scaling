package dev.apothbossscaling.client;

import dev.apothbossscaling.net.ConfigSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Cache client-side do último snapshot recebido do servidor.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientConfigCache {

    private static ConfigSnapshotPayload latest;

    private ClientConfigCache() {}

    public static void update(ConfigSnapshotPayload payload) {
        latest = payload;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ScalingOverviewScreen screen) {
            screen.onSnapshotUpdate();
        }
    }

    public static ConfigSnapshotPayload latest() {
        return latest;
    }
}
