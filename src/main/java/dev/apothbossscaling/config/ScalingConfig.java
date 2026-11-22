package dev.apothbossscaling.config;

import dev.apothbossscaling.ApotheosisBossScaling;
import dev.apothbossscaling.attachment.ScalingAttachments;
import dev.apothbossscaling.net.ConfigSnapshotPayload;
import dev.apothbossscaling.scaling.EntityClassifier;
import dev.apothbossscaling.tier.TierResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuração do mod (NeoForge {@link ModConfigSpec}, formato TOML).
 * <p>
 * Usa {@link ModConfig.Type#SERVER} (autoritativa no servidor, sincronizada aos clientes). A edição in-game
 * acontece por {@link #applyUpdate(String, String)}, que muta e persiste a config via
 * {@link ModConfigSpec.ConfigValue#set(Object)} + {@link ModConfigSpec#save()}.
 */
public final class ScalingConfig {

    /** Tiers do Apotheosis, em ordem (mesmos nomes de {@code WorldTier#getSerializedName()}). */
    public static final String[] TIERS = { "haven", "frontier", "ascent", "summit", "pinnacle" };

    private static final double[] DEFAULT_HEALTH = { 1.0, 1.08, 1.16, 1.25, 1.35 };
    private static final double[] DEFAULT_DAMAGE = { 1.0, 1.05, 1.1, 1.15, 1.2 };
    private static final double[] DEFAULT_ARMOR = { 1.0, 1.05, 1.1, 1.15, 1.2 };
    private static final double[] DEFAULT_BOSS_HEALTH = { 1.0, 1.1, 1.2, 1.35, 1.5 };
    private static final double[] DEFAULT_BOSS_DAMAGE = { 1.0, 1.08, 1.15, 1.25, 1.35 };
    private static final double[] DEFAULT_BOSS_ARMOR = { 1.0, 1.05, 1.12, 1.2, 1.3 };

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue SCALE_COMMON_MOBS;
    private static final ModConfigSpec.BooleanValue SCALE_BOSSES;
    private static final ModConfigSpec.BooleanValue SCALE_MAX_HEALTH;
    private static final ModConfigSpec.BooleanValue SCALE_ATTACK_DAMAGE;
    private static final ModConfigSpec.BooleanValue SCALE_ARMOR;
    private static final ModConfigSpec.BooleanValue DEBUG_LOG;
    private static final ModConfigSpec.DoubleValue BOSS_HEALTH_THRESHOLD;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITIES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BOSS_ENTITIES;
    private static final ModConfigSpec.BooleanValue PLAYER_SCALING_ENABLED;
    private static final ModConfigSpec.DoubleValue PLAYER_SCALING_RADIUS;
    private static final ModConfigSpec.DoubleValue PLAYER_SCALING_EXPONENT;
    private static final ModConfigSpec.BooleanValue PLAYER_SCALING_DYNAMIC;
    private static final ModConfigSpec.DoubleValue BOSS_ARMOR_CAP;
    private static final ModConfigSpec.DoubleValue MOB_ARMOR_CAP;
    private static final ModConfigSpec.DoubleValue MOD_MULT_MINECRAFT;
    private static final ModConfigSpec.DoubleValue MOD_MULT_DEFAULT;
    private static final ModConfigSpec.DoubleValue MOD_MULT_APOTHEOSIS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MOD_MULT_OVERRIDES;

    private static final Map<String, ModConfigSpec.DoubleValue> MAX_HEALTH_MULT = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> ATTACK_DAMAGE_MULT = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> ARMOR_MULT = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> BOSS_MAX_HEALTH_MULT = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> BOSS_ATTACK_DAMAGE_MULT = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> BOSS_ARMOR_MULT = new HashMap<>();

    static {
        BUILDER.push("general");
        ENABLED = BUILDER.comment("Interruptor geral do scaling.").define("enabled", true);
        SCALE_COMMON_MOBS = BUILDER.comment("Escala monstros comuns (hostis que não são boss).").define("scale_common_mobs", true);
        SCALE_BOSSES = BUILDER.comment("Escala bosses.").define("scale_bosses", true);
        SCALE_MAX_HEALTH = BUILDER.comment("Escala Max Health (modificador de atributo).").define("max_health", true);
        SCALE_ATTACK_DAMAGE = BUILDER.comment("Escala Attack Damage (multiplicador aplicado no evento de dano).").define("attack_damage", true);
        SCALE_ARMOR = BUILDER.comment("Escala Armor (multiplicador de atributo).").define("armor", true);
        DEBUG_LOG = BUILDER.comment("Logs detalhados de scaling (entidade, tier e multiplicadores).").define("debug_log", false);
        EXCLUDED_ENTITIES = BUILDER
            .comment("Entidades excluídas do scaling (ex.: \"minecraft:zombie\"). Formato \"modid:entidade\".")
            .defineListAllowEmpty("excluded_entities", List.of(), () -> "", o -> o instanceof String);
        BOSS_ENTITIES = BUILDER
            .comment("Entidades tratadas explicitamente como boss (ex.: \"iceandfire:fire_dragon\"). Formato \"modid:entidade\".")
            .defineListAllowEmpty("boss_entities", List.of(), () -> "", o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("boss_detection");
        BOSS_HEALTH_THRESHOLD = BUILDER
            .comment("Vida base máxima mínima (antes do scaling) para classificar uma entidade como boss.")
            .defineInRange("health_threshold", 100.0, 0.0, Double.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("player_scaling");
        PLAYER_SCALING_ENABLED = BUILDER
            .comment("Escala vida/dano conforme o número de jogadores próximos.")
            .define("enabled", true);
        PLAYER_SCALING_RADIUS = BUILDER
            .comment("Raio (blocos) para contar jogadores próximos.")
            .defineInRange("radius", 100.0, 1.0, 512.0);
        PLAYER_SCALING_EXPONENT = BUILDER
            .comment("Expoente da fórmula: multiplicador = jogadoresPróximos ^ expoente (0.5 = raiz quadrada, como o ScalingMobs).")
            .defineInRange("exponent", 0.5, 0.0, 4.0);
        PLAYER_SCALING_DYNAMIC = BUILDER
            .comment("Reavalia dinamicamente conforme jogadores se aproximam/afastam.")
            .define("dynamic", true);
        BUILDER.pop();

        BUILDER.push("limits");
        BOSS_ARMOR_CAP = BUILDER
            .comment("Teto máximo da armadura produzida pelo nosso scaling para BOSSES (30 = 80% de redução de dano).")
            .defineInRange("boss_armor_cap", 30.0, 0.0, 100.0);
        MOB_ARMOR_CAP = BUILDER
            .comment("Teto máximo da armadura produzida pelo nosso scaling para MOBS COMUNS (armadura alta 'mura' armas de dano baixo).")
            .defineInRange("mob_armor_cap", 10.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("mod_scaling");
        MOD_MULT_MINECRAFT = BUILDER
            .comment("Multiplicador extra para mobs vanilla (minecraft:*) aplicado sobre o multiplicador do tier.")
            .defineInRange("minecraft", 1.0, 0.0, 100.0);
        MOD_MULT_DEFAULT = BUILDER
            .comment("Multiplicador extra para mobs de outros mods (fallback) aplicado sobre o multiplicador do tier.")
            .defineInRange("default", 1.2, 0.0, 100.0);
        MOD_MULT_APOTHEOSIS = BUILDER
            .comment("Multiplicador extra para bosses do Apotheosis (Invaders/Elites), aplicado sobre o multiplicador do tier.")
            .defineInRange("apotheosis", 0.7, 0.0, 100.0);
        MOD_MULT_OVERRIDES = BUILDER
            .comment("Overrides por mod no formato \"modid:multiplicador\" (ex.: \"iceandfire:1.7\").")
            .defineListAllowEmpty("overrides", List.of(), () -> "", o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("multipliers");
        for (int i = 0; i < TIERS.length; i++) {
            String tier = TIERS[i];
            BUILDER.push(tier);
            MAX_HEALTH_MULT.put(tier, BUILDER
                .comment("Multiplicador de Max Health no tier " + tier + ".")
                .defineInRange("max_health", DEFAULT_HEALTH[i], 0.0, 1000.0));
            ATTACK_DAMAGE_MULT.put(tier, BUILDER
                .comment("Multiplicador de Attack Damage no tier " + tier + ".")
                .defineInRange("attack_damage", DEFAULT_DAMAGE[i], 0.0, 1000.0));
            ARMOR_MULT.put(tier, BUILDER
                .comment("Multiplicador de Armor no tier " + tier + ".")
                .defineInRange("armor", DEFAULT_ARMOR[i], 0.0, 1000.0));
            BUILDER.pop();
        }
        BUILDER.pop();

        BUILDER.push("boss_multipliers");
        for (int i = 0; i < TIERS.length; i++) {
            String tier = TIERS[i];
            BUILDER.push(tier);
            BOSS_MAX_HEALTH_MULT.put(tier, BUILDER
                .comment("Multiplicador de Max Health para BOSSES no tier " + tier + ".")
                .defineInRange("max_health", DEFAULT_BOSS_HEALTH[i], 0.0, 1000.0));
            BOSS_ATTACK_DAMAGE_MULT.put(tier, BUILDER
                .comment("Multiplicador de Attack Damage para BOSSES no tier " + tier + ".")
                .defineInRange("attack_damage", DEFAULT_BOSS_DAMAGE[i], 0.0, 1000.0));
            BOSS_ARMOR_MULT.put(tier, BUILDER
                .comment("Multiplicador de Armor para BOSSES no tier " + tier + ".")
                .defineInRange("armor", DEFAULT_BOSS_ARMOR[i], 0.0, 1000.0));
            BUILDER.pop();
        }
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ScalingConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    // ---- getters ----

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static boolean scaleCommonMobs() {
        return SCALE_COMMON_MOBS.get();
    }

    public static boolean scaleBosses() {
        return SCALE_BOSSES.get();
    }

    public static boolean scaleMaxHealth() {
        return SCALE_MAX_HEALTH.get();
    }

    public static boolean scaleAttackDamage() {
        return SCALE_ATTACK_DAMAGE.get();
    }

    public static boolean scaleArmor() {
        return SCALE_ARMOR.get();
    }

    public static boolean debugLog() {
        return DEBUG_LOG.get();
    }

    public static double bossHealthThreshold() {
        return BOSS_HEALTH_THRESHOLD.get();
    }

    public static List<String> excludedEntities() {
        return List.copyOf(EXCLUDED_ENTITIES.get());
    }

    public static List<String> bossEntities() {
        return List.copyOf(BOSS_ENTITIES.get());
    }

    public static boolean playerScalingEnabled() {
        return PLAYER_SCALING_ENABLED.get();
    }

    public static double playerScalingRadius() {
        return PLAYER_SCALING_RADIUS.get();
    }

    public static double playerScalingExponent() {
        return PLAYER_SCALING_EXPONENT.get();
    }

    public static boolean playerScalingDynamic() {
        return PLAYER_SCALING_DYNAMIC.get();
    }

    public static double bossArmorCap() {
        return BOSS_ARMOR_CAP.get();
    }

    public static double mobArmorCap() {
        return MOB_ARMOR_CAP.get();
    }

    public static double modMultMinecraft() {
        return MOD_MULT_MINECRAFT.get();
    }

    public static double modMultDefault() {
        return MOD_MULT_DEFAULT.get();
    }

    public static double modMultApotheosis() {
        return MOD_MULT_APOTHEOSIS.get();
    }

    public static List<String> modMultOverrides() {
        return List.copyOf(MOD_MULT_OVERRIDES.get());
    }

    /** Resolve o multiplicador do mod a partir do id da entidade (ex.: "minecraft:zombie"). */
    public static double modMultiplier(String entityId) {
        String namespace = entityId;
        int colon = entityId.indexOf(':');
        if (colon >= 0) {
            namespace = entityId.substring(0, colon);
        }
        for (String entry : MOD_MULT_OVERRIDES.get()) {
            int idx = entry.indexOf(':');
            if (idx > 0 && entry.substring(0, idx).equals(namespace)) {
                try {
                    return Double.parseDouble(entry.substring(idx + 1).trim());
                } catch (NumberFormatException ignored) {
                    // entrada inválida, ignora
                }
            }
        }
        return "minecraft".equals(namespace) ? MOD_MULT_MINECRAFT.get() : MOD_MULT_DEFAULT.get();
    }

    public static double maxHealthMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = MAX_HEALTH_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static double attackDamageMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = ATTACK_DAMAGE_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static double armorMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = ARMOR_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static double bossMaxHealthMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = BOSS_MAX_HEALTH_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static double bossAttackDamageMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = BOSS_ATTACK_DAMAGE_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static double bossArmorMultiplier(String tier) {
        ModConfigSpec.DoubleValue v = BOSS_ARMOR_MULT.get(tier);
        return v == null ? 1.0 : v.get();
    }

    public static void debug(String msg, Object... args) {
        if (DEBUG_LOG.get()) {
            ApotheosisBossScaling.LOGGER.debug("[ApothBossScaling] " + msg, args);
        }
    }

    // ---- edição in-game + snapshot ----

    /**
     * Aplica uma atualização de config vinda do cliente. Valida e persiste. Retorna {@code true} se aplicou.
     */
    public static boolean applyUpdate(String field, String value) {
        try {
            switch (field) {
                case "enabled" -> ENABLED.set(Boolean.parseBoolean(value));
                case "scale_common_mobs" -> SCALE_COMMON_MOBS.set(Boolean.parseBoolean(value));
                case "scale_bosses" -> SCALE_BOSSES.set(Boolean.parseBoolean(value));
                case "scale_max_health" -> SCALE_MAX_HEALTH.set(Boolean.parseBoolean(value));
                case "scale_attack_damage" -> SCALE_ATTACK_DAMAGE.set(Boolean.parseBoolean(value));
                case "scale_armor" -> SCALE_ARMOR.set(Boolean.parseBoolean(value));
                case "debug_log" -> DEBUG_LOG.set(Boolean.parseBoolean(value));
                case "boss_health_threshold" -> BOSS_HEALTH_THRESHOLD.set(clamp(Double.parseDouble(value), 0.0, Double.MAX_VALUE));
                case "excluded_entities" -> EXCLUDED_ENTITIES.set(parseList(value));
                case "boss_entities" -> BOSS_ENTITIES.set(parseList(value));
                case "player_scaling.enabled" -> PLAYER_SCALING_ENABLED.set(Boolean.parseBoolean(value));
                case "player_scaling.radius" -> PLAYER_SCALING_RADIUS.set(clamp(Double.parseDouble(value), 1.0, 512.0));
                case "player_scaling.exponent" -> PLAYER_SCALING_EXPONENT.set(clamp(Double.parseDouble(value), 0.0, 4.0));
                case "player_scaling.dynamic" -> PLAYER_SCALING_DYNAMIC.set(Boolean.parseBoolean(value));
                case "boss_armor_cap" -> BOSS_ARMOR_CAP.set(clamp(Double.parseDouble(value), 0.0, 100.0));
                case "mob_armor_cap" -> MOB_ARMOR_CAP.set(clamp(Double.parseDouble(value), 0.0, 100.0));
                case "mod_scaling.minecraft" -> MOD_MULT_MINECRAFT.set(clamp(Double.parseDouble(value), 0.0, 100.0));
                case "mod_scaling.default" -> MOD_MULT_DEFAULT.set(clamp(Double.parseDouble(value), 0.0, 100.0));
                case "mod_scaling.apotheosis" -> MOD_MULT_APOTHEOSIS.set(clamp(Double.parseDouble(value), 0.0, 100.0));
                case "mod_scaling.overrides" -> MOD_MULT_OVERRIDES.set(parseList(value));
                case "reset_tiers" -> {
                    for (int i = 0; i < TIERS.length; i++) {
                        String tier = TIERS[i];
                        MAX_HEALTH_MULT.get(tier).set(DEFAULT_HEALTH[i]);
                        ATTACK_DAMAGE_MULT.get(tier).set(DEFAULT_DAMAGE[i]);
                        ARMOR_MULT.get(tier).set(DEFAULT_ARMOR[i]);
                    }
                }
                case "reset_boss_tiers" -> {
                    for (int i = 0; i < TIERS.length; i++) {
                        String tier = TIERS[i];
                        BOSS_MAX_HEALTH_MULT.get(tier).set(DEFAULT_BOSS_HEALTH[i]);
                        BOSS_ATTACK_DAMAGE_MULT.get(tier).set(DEFAULT_BOSS_DAMAGE[i]);
                        BOSS_ARMOR_MULT.get(tier).set(DEFAULT_BOSS_ARMOR[i]);
                    }
                }
                case "preset" -> {
                    Presets.Preset preset = Presets.byId(value);
                    if (preset == null) {
                        return false;
                    }
                    for (int i = 0; i < TIERS.length; i++) {
                        String tier = TIERS[i];
                        MAX_HEALTH_MULT.get(tier).set(preset.commonHealth()[i]);
                        ATTACK_DAMAGE_MULT.get(tier).set(preset.commonDamage()[i]);
                        ARMOR_MULT.get(tier).set(preset.commonArmor()[i]);
                        BOSS_MAX_HEALTH_MULT.get(tier).set(preset.bossHealth()[i]);
                        BOSS_ATTACK_DAMAGE_MULT.get(tier).set(preset.bossDamage()[i]);
                        BOSS_ARMOR_MULT.get(tier).set(preset.bossArmor()[i]);
                    }
                }
                default -> {
                    if (field.startsWith("max_health.")) {
                        String tier = field.substring("max_health.".length());
                        ModConfigSpec.DoubleValue v = MAX_HEALTH_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else if (field.startsWith("attack_damage.")) {
                        String tier = field.substring("attack_damage.".length());
                        ModConfigSpec.DoubleValue v = ATTACK_DAMAGE_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else if (field.startsWith("armor.")) {
                        String tier = field.substring("armor.".length());
                        ModConfigSpec.DoubleValue v = ARMOR_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else if (field.startsWith("boss_max_health.")) {
                        String tier = field.substring("boss_max_health.".length());
                        ModConfigSpec.DoubleValue v = BOSS_MAX_HEALTH_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else if (field.startsWith("boss_attack_damage.")) {
                        String tier = field.substring("boss_attack_damage.".length());
                        ModConfigSpec.DoubleValue v = BOSS_ATTACK_DAMAGE_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else if (field.startsWith("boss_armor.")) {
                        String tier = field.substring("boss_armor.".length());
                        ModConfigSpec.DoubleValue v = BOSS_ARMOR_MULT.get(tier);
                        if (v == null) {
                            return false;
                        }
                        v.set(clamp(Double.parseDouble(value), 0.0, 1000.0));
                    } else {
                        return false;
                    }
                }
            }
            SPEC.save();
            return true;
        }
        catch (Exception e) {
            ApotheosisBossScaling.LOGGER.warn("Falha ao aplicar atualização de config ({} = {}): {}", field, value, e.getMessage());
            return false;
        }
    }

    /** Constrói o snapshot enviado ao cliente (config + tier do jogador + entidades escaladas próximas). */
    public static ConfigSnapshotPayload buildSnapshot(ServerPlayer player) {
        String tier = TierResolver.tierName(player);
        return new ConfigSnapshotPayload(
            isEnabled(), scaleCommonMobs(), scaleBosses(), scaleMaxHealth(), scaleAttackDamage(), scaleArmor(), debugLog(),
            bossHealthThreshold(), bossArmorCap(), mobArmorCap(),
            playerScalingEnabled(), playerScalingRadius(), playerScalingExponent(), playerScalingDynamic(),
            modMultMinecraft(), modMultDefault(), modMultApotheosis(), String.join(",", modMultOverrides()),
            String.join(",", excludedEntities()),
            String.join(",", bossEntities()),
            maxHealthMultiplierList(), attackDamageMultiplierList(), armorMultiplierList(),
            bossMaxHealthMultiplierList(), bossAttackDamageMultiplierList(), bossArmorMultiplierList(),
            tier,
            nearbyScaled(player),
            modNamespaces());
    }

    private static List<Double> maxHealthMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(maxHealthMultiplier(t));
        }
        return list;
    }

    private static List<Double> attackDamageMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(attackDamageMultiplier(t));
        }
        return list;
    }

    private static List<Double> armorMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(armorMultiplier(t));
        }
        return list;
    }

    private static List<Double> bossMaxHealthMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(bossMaxHealthMultiplier(t));
        }
        return list;
    }

    private static List<Double> bossAttackDamageMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(bossAttackDamageMultiplier(t));
        }
        return list;
    }

    private static List<Double> bossArmorMultiplierList() {
        List<Double> list = new ArrayList<>(TIERS.length);
        for (String t : TIERS) {
            list.add(bossArmorMultiplier(t));
        }
        return list;
    }

    private static List<ConfigSnapshotPayload.EntityInfo> nearbyScaled(ServerPlayer player) {
        List<ConfigSnapshotPayload.EntityInfo> list = new ArrayList<>();
        AABB box = player.getBoundingBox().inflate(64.0);
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, box, m -> !m.getData(ScalingAttachments.SCALED_TIER.get()).isEmpty())) {
            String tier = mob.getData(ScalingAttachments.SCALED_TIER.get());
            list.add(new ConfigSnapshotPayload.EntityInfo(
                EntityClassifier.entityId(mob), tier, maxHealthMultiplier(tier), attackDamageMultiplier(tier)));
            if (list.size() >= 64) {
                break;
            }
        }
        return list;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Namespaces (mods) que registram entidades, em ordem alfabética (exceto "minecraft"). */
    private static List<String> modNamespaces() {
        TreeSet<String> namespaces = new TreeSet<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (key != null && !"minecraft".equals(key.getNamespace())) {
                namespaces.add(key.getNamespace());
            }
        }
        return new ArrayList<>(namespaces);
    }
}
