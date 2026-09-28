package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Убираем солнце и луну из неба во время X-Ray.
 *
 * В Minecraft 1.21.1 эти текстуры хранятся в статических полях
 * LevelRenderer и используются непосредственно внутри renderSky().
 * Временно подменяем именно эти ссылки на прозрачную текстуру.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    private static final ResourceLocation TRANSPARENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("xtoxray", "textures/misc/transparent.png");

    @Shadow @Final @Mutable
    private static ResourceLocation SUN_LOCATION;

    @Shadow @Final @Mutable
    private static ResourceLocation MOON_LOCATION;

    private static ResourceLocation xtoxray$originalSun;
    private static ResourceLocation xtoxray$originalMoon;

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void xtoxray$hideSunMoon(CallbackInfo ci) {
        if (!XrayState.getInstance().isActive()) {
            return;
        }

        xtoxray$originalSun = SUN_LOCATION;
        xtoxray$originalMoon = MOON_LOCATION;

        SUN_LOCATION = TRANSPARENT_TEXTURE;
        MOON_LOCATION = TRANSPARENT_TEXTURE;
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void xtoxray$restoreSunMoon(CallbackInfo ci) {
        if (xtoxray$originalSun != null) {
            SUN_LOCATION = xtoxray$originalSun;
            xtoxray$originalSun = null;
        }

        if (xtoxray$originalMoon != null) {
            MOON_LOCATION = xtoxray$originalMoon;
            xtoxray$originalMoon = null;
        }
    }
}
