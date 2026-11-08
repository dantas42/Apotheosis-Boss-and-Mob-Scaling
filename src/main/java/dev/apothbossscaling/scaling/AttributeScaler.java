package dev.apothbossscaling.scaling;

import dev.apothbossscaling.ApotheosisBossScaling;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Aplica/remove modificadores de atributo de forma <b>idempotente</b>.
 * <p>
 * O ID do modificador é <b>independente de tier</b> (ex.: {@code apothboss_scaling:max_health}), para que a
 * reaplicação com um tier diferente <b>substitua</b> o modificador anterior em vez de acumular (via
 * {@link AttributeInstance#addOrReplacePermanentModifier(AttributeModifier)}). O tier aplicado é registrado à
 * parte em {@code ScalingAttachments.SCALED_TIER}.
 */
public final class AttributeScaler {

    private AttributeScaler() {}

    /** Aplica (ou remove, se multiplier == 1.0) o modificador de Max Health. */
    public static void applyMaxHealth(LivingEntity entity, double multiplier) {
        apply(entity, Attributes.MAX_HEALTH, "max_health", multiplier);
    }

    /**
     * Aplica o modificador de Armor com teto. Como a redução de dano satura em {@code cap}
     * (30 = 80%), usa um bônus flat (ADD_VALUE) que leva a armadura até {@code min(base * multiplier, cap)}.
     * Isso evita valores absurdos (ex.: 156) que não mudam a redução de dano além do teto.
     */
    public static void applyArmor(LivingEntity entity, double multiplier, double cap) {
        AttributeInstance instance = entity.getAttribute(Attributes.ARMOR);
        if (instance == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ApotheosisBossScaling.MODID, "armor");
        AttributeModifier mine = instance.getModifier(id);
        double withoutMine = mine == null ? instance.getValue() : instance.getValue() - mine.amount();
        double target = Math.min(withoutMine * multiplier, cap);
        double amount = target - withoutMine;
        if (amount <= 0.0001) {
            instance.removeModifier(id);
            return;
        }
        AttributeModifier modifier = new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
        instance.addOrReplacePermanentModifier(modifier);
    }

    private static void apply(LivingEntity entity, Holder<Attribute> attribute, String attrName, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return; // entidade não declara o atributo — ignora graciosamente
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ApotheosisBossScaling.MODID, attrName);
        if (!needsScaling(multiplier)) {
            instance.removeModifier(id);
            return;
        }
        // ADD_MULTIPLIED_BASE: valor final = base * (1 + amount). Para multiplicar por "multiplier", amount = multiplier - 1.
        AttributeModifier modifier = new AttributeModifier(id, modifierAmount(multiplier), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        instance.addOrReplacePermanentModifier(modifier);
    }

    /** Quantia do modificador para um dado multiplicador (exposto para testes). */
    public static double modifierAmount(double multiplier) {
        return multiplier - 1.0;
    }

    /** Se um multiplicador exige aplicação de modificador (qualquer valor diferente de 1.0). */
    public static boolean needsScaling(double multiplier) {
        return multiplier != 1.0;
    }
}
