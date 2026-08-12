package com.arkflame.flamepearls.config;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GeneralConfigHolderModernCooldownTest {
    @Test
    public void defaultsModernCooldownToEnabled() {
        final GeneralConfigHolder holder = new GeneralConfigHolder();
        holder.load(new MemoryConfiguration());

        assertTrue(holder.isModernCooldownEnabled());
    }

    @Test
    public void readsExplicitModernCooldownDisabled() {
        final MemoryConfiguration config = new MemoryConfiguration();
        config.set("cooldown.modern-cooldown", false);
        final GeneralConfigHolder holder = new GeneralConfigHolder();
        holder.load(config);

        assertFalse(holder.isModernCooldownEnabled());
    }
}
