package com.floweytf.tabscroll.scrollabletab.compat;

import com.floweytf.tabscroll.scrollabletab.compat.betterping.BetterPingCompatHandler;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class CompatHandler {
    private static final Map<String, ICompatHandler> COMPAT_HANDLERS = Map.ofEntries(
        Map.entry("betterpingdisplay", new BetterPingCompatHandler())
    );
    private static final Logger LOGGER = LogManager.getLogger("ScrollableTagCompat");

    public static void setup() {
        COMPAT_HANDLERS.forEach((mod, handler) -> {
            if(FabricLoader.getInstance().isModLoaded(mod)) {
                LOGGER.info("Setting up compat for mod " + mod);
                handler.setup();
            }
        });
    }
}
