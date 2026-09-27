package com.xtoxray.client.mixin;

import com.xtoxray.client.gui.XrayBlockPickerScreen;
import com.xtoxray.client.gui.XrayConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    /*
     * Minecraft применяет общий post-process blur независимо от того,
     * вызвало ли конкретное окно renderBlurredBackground().
     * Для интерфейса XtoXray этот эффект не нужен.
     */
    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
    private void xtoxray$disableMenuBlur(float delta, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof XrayConfigScreen
                || minecraft.screen instanceof XrayBlockPickerScreen) {
            callback.cancel();
        }
    }
}
