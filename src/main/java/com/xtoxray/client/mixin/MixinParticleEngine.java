package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * Скрываем весь обычный рендер частиц во время X-Ray.
 *
 * Сами частицы продолжают обновляться, поэтому включение/выключение
 * X-Ray не меняет игровую логику, а только их отображение.
 */
@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void xtoxray$hideParticles(
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            Frustum frustum,
            Predicate<ParticleRenderType> renderTypePredicate,
            CallbackInfo ci
    ) {
        if (XrayState.getInstance().isActive()) {
            ci.cancel();
        }
    }
}
