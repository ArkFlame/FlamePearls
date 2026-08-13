package com.arkflame.flamepearls.compat.endermite;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public final class EndermiteSpawnBridgeFactoryTest {
    @Test
    public void compileTargetExposesReasonAwareEndermiteSpawnCapability() {
        final EndermiteSpawnBridge bridge = EndermiteSpawnBridgeFactory.create(
                Logger.getLogger(EndermiteSpawnBridgeFactoryTest.class.getName())
        );
        if (!bridge.preservesEnderPearlReason()) {
            throw new AssertionError("Target API must expose a reason-aware endermite spawn capability");
        }
    }
}
