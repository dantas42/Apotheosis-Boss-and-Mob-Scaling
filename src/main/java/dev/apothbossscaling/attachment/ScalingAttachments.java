package dev.apothbossscaling.attachment;

import com.mojang.serialization.Codec;
import dev.apothbossscaling.ApotheosisBossScaling;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Anexos (attachments) persistentes usados para deduplicar e reaplicar o scaling.
 * <p>
 * Diferente do Apotheosis (que guarda apenas um booleano {@code TIER_AUGMENTS_APPLIED}), guardamos o
 * <b>tier aplicado</b> e o <b>multiplicador de dano</b>, permitindo reaplicar os modificadores de forma
 * idempotente após save/load, chunk load, teletransporte e troca de dimensão — mesmo sem persistência de
 * modificadores de atributo no NeoForge.
 */
public final class ScalingAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ApotheosisBossScaling.MODID);

    /**
     * Nome do tier aplicado à entidade (ex.: "pinnacle"). Vazio ("" ) significa que a entidade ainda não foi escalada.
     * Persistido para permitir reaplicação idempotente independente de jogador próximo.
     */
    public static final Supplier<AttachmentType<String>> SCALED_TIER =
        ATTACHMENT_TYPES.register("scaled_tier", () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /**
     * Multiplicador de dano resolvido para a entidade (1.0 = sem escala). Lido no evento de dano.
     * Persistido para sobreviver a reload.
     */
    public static final Supplier<AttachmentType<Float>> DAMAGE_MULTIPLIER =
        ATTACHMENT_TYPES.register("damage_multiplier", () -> AttachmentType.builder(() -> 1.0F).serialize(Codec.FLOAT).build());

    /**
     * Se a entidade foi classificada como boss. Usado para aplicar o scaling por jogadores apenas em boss.
     * Persistido para a classificação não mudar quando a vida já está escalada.
     */
    public static final Supplier<AttachmentType<Boolean>> IS_BOSS =
        ATTACHMENT_TYPES.register("is_boss", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    private ScalingAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
