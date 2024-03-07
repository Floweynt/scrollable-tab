package com.floweytf.tabscroll.scrollabletab;

import com.floweytf.tabscroll.scrollabletab.compat.CompatHandler;
import net.fabricmc.api.ClientModInitializer;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        CompatHandler.setup();
    }
}
