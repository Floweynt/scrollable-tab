package com.floweytf.tabscroll.scrollabletab.mixin;

import com.floweytf.tabscroll.scrollabletab.Config;
import com.floweytf.tabscroll.scrollabletab.IPlayerTabOverlay;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(
        method = "onScroll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"
        ),
        cancellable = true
    )
    private void scrollable_tab$handleScroll(long l, double d, double e, CallbackInfo ci, @Local(ordinal = 2) int scrollAmount) {
        var tab = (IPlayerTabOverlay) minecraft.gui.getTabList();
        if(tab.scrollable_tab$isVisible()) {
            tab.scrollable_tab$handle(scrollAmount * Config.getInstance().scrollbarIncrement);
            ci.cancel();
        }
    }
}
