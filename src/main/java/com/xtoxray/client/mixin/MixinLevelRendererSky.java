package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Убираем солнце и луну из неба во время X-Ray.
 *
 * В Minecraft 1.21.1 LevelRenderer хранит текстуры солнца и луны
 * в статических полях и читает их непосредственно внутри renderSky().
 * Вместо изменения самих static final полей перехватываем именно
 * чтение этих полей в renderSky(). Это не требует временной мутации
 * полей и меньше конфликтует с другими миксинами, включая Iris.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    private static final ResourceLocation TRANSPARENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("xtoxray", "textures/misc/transparent.png");

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;SUN_LOCATION:Lnet/minecraft/resources/ResourceLocation;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static ResourceLocation xtoxray$redirectSunTexture() {
        return XrayState.getInstance().isActive()
            ? TRANSPARENT_TEXTURE
            : getSunTexture();
    }

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;MOON_LOCATION:Lnet/minecraft/resources/ResourceLocation;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static ResourceLocation xtoxray$redirectMoonTexture() {
        return XrayState.getInstance().isActive()
            ? TRANSPARENT_TEXTURE
            : getMoonTexture();
    }

    @org.spongepowered.asm.mixin.Shadow
    private static ResourceLocation getSunTexture() {
        throw new AssertionError();
    }

    @org.spongepowered.asm.mixin.Shadow
    private static ResourceLocation getMoonTexture() {
        throw new AssertionError();
    }
}
