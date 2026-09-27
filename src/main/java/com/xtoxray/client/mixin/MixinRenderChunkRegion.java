package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
public abstract class MixinRenderChunkRegion {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void xtoxray$filter(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        XrayState xray = XrayState.getInstance();

        if (!xray.isActive() || state == null || state.isAir()) {
            return;
        }

        if (!xray.shouldRender(state)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
        }

        int distance = xray.getOreRenderDistance();
        Minecraft mc = Minecraft.getInstance();
        if (distance > 0 && mc.player != null && mc.level != null) {
            if (pos.distSqr(mc.player.blockPosition()) >= (double) distance * distance) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }
}
