package com.arkflame.flamepearls.handlers;

import com.arkflame.flamepearls.config.GeneralConfigHolder;
import com.arkflame.flamepearls.managers.OriginManager;
import com.arkflame.flamepearls.services.PearlTeleportService;
import com.arkflame.flamepearls.utils.FoliaAPI;

public final class PearlFixerHandlerFactory {
    private PearlFixerHandlerFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static PearlFixerHandler create(final OriginManager originManager,
                                           final GeneralConfigHolder generalConfigHolder,
                                           final PearlTeleportService pearlTeleportService) {
        if (FoliaAPI.isFolia()) {
            return new FoliaPearlFixerHandler(originManager, generalConfigHolder, pearlTeleportService);
        }
        return new PaperPearlFixerHandler(originManager, generalConfigHolder, pearlTeleportService);
    }
}