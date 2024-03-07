package com.floweytf.tabscroll.scrollabletab.compat.betterping;

import com.floweytf.tabscroll.scrollabletab.compat.ICompatHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.lang.reflect.InvocationTargetException;

public class BetterPingCompatHandler implements ICompatHandler {
    public interface BetterPingCallback {
        void render(Minecraft client, PlayerTabOverlay hud, PoseStack context, int width, int x, int y, PlayerInfo player);
    }

    public static BetterPingCallback callback;

    @Override
    public void setup() {
        if (FabricLoader.getInstance().isModLoaded("betterpingdisplay")) {
            try {
                var method =
                    Class.forName("com.vladmarica.betterpingdisplay.hud.CustomPlayerListHud")
                        .getMethod(
                            "renderPingDisplay",
                            Minecraft.class, PlayerTabOverlay.class, PoseStack.class, int.class, int.class, int.class, PlayerInfo.class
                        );
                callback = (mc, h, s, w, x, y, p) -> {
                    try {
                        method.invoke(null, mc, h, s, w, x, y, p);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                };

            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
