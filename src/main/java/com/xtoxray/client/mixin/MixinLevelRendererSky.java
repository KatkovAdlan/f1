package com.xtoxray.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Убираем солнце и луну из неба во время X-Ray.
 *
 * Minecraft 1.21.1 хранит ссылки на текстуры солнца и луны в LevelRenderer.
 * Вместо попытки подменять сами static final поля перехватываем привязку
 * текстуры непосредственно в RenderSystem.setShaderTexture() внутри renderSky().
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    private static final ResourceLocation TRANSPARENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("xtoxray", "textures/misc/transparent.png");

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private static ResourceLocation SUN_LOCATION;

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private static ResourceLocation MOON_LOCATION;

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
        )
    )
    private void xtoxray$redirectSkyTexture(int textureUnit, ResourceLocation texture) {
        if (XrayState.getInstance().isActive()
                && (SUN_LOCATION.equals(texture) || MOON_LOCATION.equals(texture))) {
            RenderSystem.setShaderTexture(textureUnit, TRANSPARENT_TEXTURE);
            return;
        }

        RenderSystem.setShaderTexture(textureUnit, texture);
    }
}
