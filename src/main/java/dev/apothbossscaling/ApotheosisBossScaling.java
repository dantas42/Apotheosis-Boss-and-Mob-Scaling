package dev.apothbossscaling;

import com.mojang.logging.LogUtils;
import dev.apothbossscaling.attachment.ScalingAttachments;
import dev.apothbossscaling.config.ScalingConfig;
import dev.apothbossscaling.event.ScalingEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Apotheosis Boss Scaling.
 * <p>
 * Escala bosses e monstros de outros mods conforme o World Tier do Apotheosis.
 * Integra-se exclusivamente pela API pública do Apotheosis ({@code dev.shadowsoffire.apotheosis.tiers.WorldTier}).
 * O mod é server-side: todo o scaling ocorre no servidor e os atributos sincronizam automaticamente.
 */
@Mod(ApotheosisBossScaling.MODID)
public final class ApotheosisBossScaling {

    public static final String MODID = "apothboss_scaling";

    public static final Logger LOGGER = LogUtils.getLogger();

    public ApotheosisBossScaling(IEventBus modEventBus, ModContainer modContainer) {
        ScalingConfig.register(modContainer);
        ScalingAttachments.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ScalingEventHandler());
    }
}
