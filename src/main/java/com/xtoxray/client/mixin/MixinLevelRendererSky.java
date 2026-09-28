package com.xtoxray.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Убираем солнце и луну из неба во время X-Ray.
 *
 * Само небо и его цвет остаются ванильными. Мы подменяем только
 * текстуры солнца и луны на полностью прозрачную текстуру.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    private static final ResourceLocation SUN_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/environment/sun.png");

    private static final ResourceLocation MOON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");

    private static final ResourceLocation TRANSPARENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("xtoxray", "textures/misc/transparent.png");

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
        )
    )
    private void xtoxray$hideSunMoon(int slot, ResourceLocation texture) {
        if (XrayState.getInstance().isActive()
                && (SUN_TEXTURE.equals(texture) || MOON_TEXTURE.equals(texture))) {
            RenderSystem.setShaderTexture(slot, TRANSPARENT_TEXTURE);
            return;
        }

        RenderSystem.setShaderTexture(slot, texture);
    }
}
