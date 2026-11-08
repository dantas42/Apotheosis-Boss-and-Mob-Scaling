package dev.apothbossscaling.scaling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Testes da lógica pura de scaling (sem runtime do Minecraft).
 */
class AttributeScalerTest {

    @Test
    void modifierAmountIsMultiplierMinusOne() {
        assertEquals(0.0, AttributeScaler.modifierAmount(1.0), 1e-9, "mult 1.0 não deve escalar");
        assertEquals(3.0, AttributeScaler.modifierAmount(4.0), 1e-9, "mult 4.0 -> amount 3.0");
        assertEquals(-0.5, AttributeScaler.modifierAmount(0.5), 1e-9, "mult 0.5 -> amount -0.5");
    }

    @Test
    void needsScalingIsTrueUnlessMultiplierIsOne() {
        assertFalse(AttributeScaler.needsScaling(1.0), "1.0 é neutro");
        assertTrue(AttributeScaler.needsScaling(1.5), "1.5 escala");
        assertTrue(AttributeScaler.needsScaling(0.0), "0.0 também difere de 1.0");
    }
}
