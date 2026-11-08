package dev.apothbossscaling.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.apothbossscaling.ApotheosisBossScaling;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Registro client-side (keybind). Roda apenas no cliente.
 */
@EventBusSubscriber(modid = ApotheosisBossScaling.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ScalingClientSetup {

    public static final KeyMapping OPEN = new KeyMapping(
        "key.apothboss_scaling.open",
        KeyConflictContext.IN_GAME,
        KeyModifier.CONTROL,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_S,
        "key.categories.apothboss_scaling");

    private ScalingClientSetup() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }
}
