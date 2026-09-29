package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Полностью отключаем стандартный рендер неба во время обычного X-Ray.
 *
 * Это надёжнее, чем отдельно подменять текстуры солнца и луны:
 * renderSky() отвечает сразу за небо, солнце, луну, звёзды и остальные
 * элементы небесной сцены.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    @Inject(
        method = "renderSky",
        at = @At("HEAD"),
        cancellable = true
    )
    private void xtoxray$hideSky(CallbackInfo ci) {
        if (XrayState.getInstance().isActive()) {
            ci.cancel();
        }
    }
}
