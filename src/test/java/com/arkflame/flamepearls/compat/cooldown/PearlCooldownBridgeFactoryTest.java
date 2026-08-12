package com.arkflame.flamepearls.compat.cooldown;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public final class PearlCooldownBridgeFactoryTest {
    @Test
    public void compileTargetExposesMaterialCooldownCapability() {
        final PearlCooldownBridge bridge = PearlCooldownBridgeFactory.create(
                Logger.getLogger(PearlCooldownBridgeFactoryTest.class.getName())
        );
        if (!bridge.isSupported()) {
            throw new AssertionError("Target API must expose HumanEntity#setCooldown(Material,int)");
        }
    }
}
