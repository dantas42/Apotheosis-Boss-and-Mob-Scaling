package dev.apothbossscaling.scaling;

import dev.apothbossscaling.attachment.ScalingAttachments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Escala o dano no momento do hit, cobrindo de forma uniforme <b>melee, projéteis, magia e dano custom</b>.
 * <p>
 * Em vez de escalar apenas o atributo {@code ATTACK_DAMAGE} (que não cobre magia de mods como Iron's Spells nem
 * projéteis), multiplicamos a quantia de dano em {@link LivingIncomingDamageEvent}, que é o ponto único por onde
 * passa <b>todo</b> dano via {@code LivingEntity#hurt}. Sem dupla contagem (um único ponto de multiplicação).
 */
public final class DamageScaler {

    private DamageScaler() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity attacker = resolveAttacker(event.getSource());
        if (attacker == null) {
            return;
        }
        float multiplier = attacker.getData(ScalingAttachments.DAMAGE_MULTIPLIER.get());
        if (multiplier != 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    /** Resolve o atacante "raiz" (dono do projétil quando aplicável). */
    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity root = source.getEntity();
        if (root instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        if (direct instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
