package dev.apothbossscaling.net;

import dev.apothbossscaling.ApotheosisBossScaling;
import dev.apothbossscaling.client.ClientConfigCache;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Snapshot servidor→cliente: valores da config, tier atual do jogador, entidades escaladas próximas
 * e a lista de mods (namespaces) que registram mobs.
 */
public record ConfigSnapshotPayload(
    boolean enabled,
    boolean scaleCommonMobs,
    boolean scaleBosses,
    boolean scaleMaxHealth,
    boolean scaleAttackDamage,
    boolean scaleArmor,
    boolean debugLog,
    double bossHealthThreshold,
    double bossArmorCap,
    double mobArmorCap,
    boolean playerScalingEnabled,
    double playerScalingRadius,
    double playerScalingExponent,
    boolean playerScalingDynamic,
    double modMultMinecraft,
    double modMultDefault,
    double modMultApotheosis,
    String modMultOverrides,
    String excludedEntities,
    String bossEntities,
    List<Double> maxHealthMultipliers,
    List<Double> attackDamageMultipliers,
    List<Double> armorMultipliers,
    List<Double> bossMaxHealthMultipliers,
    List<Double> bossAttackDamageMultipliers,
    List<Double> bossArmorMultipliers,
    String currentTier,
    List<EntityInfo> nearby,
    List<String> modNamespaces) implements CustomPacketPayload {

    public static final Type<ConfigSnapshotPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ApotheosisBossScaling.MODID, "config_snapshot"));

    public static final StreamCodec<ByteBuf, ConfigSnapshotPayload> STREAM_CODEC = StreamCodec.of(
        (ByteBuf buf, ConfigSnapshotPayload p) -> {
            ByteBufCodecs.BOOL.encode(buf, p.enabled);
            ByteBufCodecs.BOOL.encode(buf, p.scaleCommonMobs);
            ByteBufCodecs.BOOL.encode(buf, p.scaleBosses);
            ByteBufCodecs.BOOL.encode(buf, p.scaleMaxHealth);
            ByteBufCodecs.BOOL.encode(buf, p.scaleAttackDamage);
            ByteBufCodecs.BOOL.encode(buf, p.scaleArmor);
            ByteBufCodecs.BOOL.encode(buf, p.debugLog);
            ByteBufCodecs.DOUBLE.encode(buf, p.bossHealthThreshold);
            ByteBufCodecs.DOUBLE.encode(buf, p.bossArmorCap);
            ByteBufCodecs.DOUBLE.encode(buf, p.mobArmorCap);
            ByteBufCodecs.BOOL.encode(buf, p.playerScalingEnabled);
            ByteBufCodecs.DOUBLE.encode(buf, p.playerScalingRadius);
            ByteBufCodecs.DOUBLE.encode(buf, p.playerScalingExponent);
            ByteBufCodecs.BOOL.encode(buf, p.playerScalingDynamic);
            ByteBufCodecs.DOUBLE.encode(buf, p.modMultMinecraft);
            ByteBufCodecs.DOUBLE.encode(buf, p.modMultDefault);
            ByteBufCodecs.DOUBLE.encode(buf, p.modMultApotheosis);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.modMultOverrides);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.excludedEntities);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.bossEntities);
            doubleList().encode(buf, p.maxHealthMultipliers);
            doubleList().encode(buf, p.attackDamageMultipliers);
            doubleList().encode(buf, p.armorMultipliers);
            doubleList().encode(buf, p.bossMaxHealthMultipliers);
            doubleList().encode(buf, p.bossAttackDamageMultipliers);
            doubleList().encode(buf, p.bossArmorMultipliers);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.currentTier);
            entityList().encode(buf, p.nearby);
            stringList().encode(buf, p.modNamespaces);
        },
        (ByteBuf buf) -> new ConfigSnapshotPayload(
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            doubleList().decode(buf),
            doubleList().decode(buf),
            doubleList().decode(buf),
            doubleList().decode(buf),
            doubleList().decode(buf),
            doubleList().decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            entityList().decode(buf),
            stringList().decode(buf)));

    private static StreamCodec<ByteBuf, List<Double>> doubleList() {
        return ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.DOUBLE);
    }

    private static StreamCodec<ByteBuf, List<String>> stringList() {
        return ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);
    }

    private static StreamCodec<ByteBuf, List<EntityInfo>> entityList() {
        return ByteBufCodecs.collection(ArrayList::new, EntityInfo.STREAM_CODEC);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Cliente: guarda o snapshot no cache (e atualiza a tela se aberta). */
    public static void handle(ConfigSnapshotPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientConfigCache.update(payload));
    }

    public record EntityInfo(String entityId, String tierName, double healthMult, double damageMult) {
        public static final StreamCodec<ByteBuf, EntityInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EntityInfo::entityId,
            ByteBufCodecs.STRING_UTF8, EntityInfo::tierName,
            ByteBufCodecs.DOUBLE, EntityInfo::healthMult,
            ByteBufCodecs.DOUBLE, EntityInfo::damageMult,
            EntityInfo::new);
    }
}
