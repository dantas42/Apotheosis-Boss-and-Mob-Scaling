package dev.apothbossscaling.client;

import com.mojang.brigadier.CommandDispatcher;
import dev.apothbossscaling.ApotheosisBossScaling;
import dev.apothbossscaling.net.RequestSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handlers client-side (keybind tick + comando para abrir a tela). Roda apenas no cliente.
 */
@EventBusSubscriber(modid = ApotheosisBossScaling.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ScalingClientEvents {

    private ScalingClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ScalingClientSetup.OPEN.consumeClick()) {
            open();
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("apothboss_scaling")
            .executes(ctx -> {
                open();
                return 1;
            }));
    }

    private static void open() {
        PacketDistributor.sendToServer(new RequestSnapshotPayload());
        Minecraft.getInstance().setScreen(new ScalingOverviewScreen());
    }
}
