package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {
    @Inject(
        method = "renderSky",
        at = @At("HEAD"),
        cancellable = true
    )
    private void xtoxray$hideSky(CallbackInfo ci) {
        if (XrayState.getInstance().isBlockFilterActive()) {
            ci.cancel();
        }
    }
}