package com.arkflame.flamepearls.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CooldownManagerTickConversionTest {
    @Test
    public void convertsZeroToZeroTicks() {
        assertEquals(0, CooldownManager.toCooldownTicks(0.0D));
    }

    @Test
    public void roundsSmallPositiveCooldownUp() {
        assertEquals(1, CooldownManager.toCooldownTicks(0.001D));
    }

    @Test
    public void convertsOneSecondToTwentyTicks() {
        assertEquals(20, CooldownManager.toCooldownTicks(1.0D));
    }

    @Test
    public void roundsFractionalCooldownUp() {
        assertEquals(199, CooldownManager.toCooldownTicks(9.949D));
    }

    @Test
    public void clampsPositiveInfinityToMaximumTicks() {
        assertEquals(Integer.MAX_VALUE, CooldownManager.toCooldownTicks(Double.POSITIVE_INFINITY));
    }
}
