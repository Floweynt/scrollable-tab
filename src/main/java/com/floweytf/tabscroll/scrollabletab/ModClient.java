package com.floweytf.tabscroll.scrollabletab;

import net.fabricmc.api.ClientModInitializer;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
    }
}
