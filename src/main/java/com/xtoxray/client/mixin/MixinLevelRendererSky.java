package com.xtoxray.client.mixin;

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
 * В Minecraft 1.21.1 LevelRenderer хранит текстуры солнца и луны
 * в статических полях и читает их непосредственно внутри renderSky().
 * Вместо временной мутации static final полей перехватываем именно
 * чтение этих полей в renderSky(). Это оставляет исходные поля
 * неизменными и уменьшает риск конфликтов с другими миксинами.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererSky {

    private static final ResourceLocation TRANSPARENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("xtoxray", "textures/misc/transparent.png");

    @Shadow
    private static ResourceLocation SUN_LOCATION;

    @Shadow
    private static ResourceLocation MOON_LOCATION;

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
            : SUN_LOCATION;
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
            : MOON_LOCATION;
    }
}
