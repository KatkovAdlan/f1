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
    private void xtoxray$filter(BlockPos pos, CallbackInfoReturnable<BlockState> callback) {
        BlockState state = callback.getReturnValue();
        XrayState xray = XrayState.getInstance();

        if (!xray.isBlockFilterActive() || state == null || state.isAir()) {
            return;
        }

        if (!xray.shouldRender(state)) {
            callback.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
        }

        int distance = xray.getOreRenderDistance();
        Minecraft minecraft = Minecraft.getInstance();

        if (distance > 0 && minecraft.player != null) {
            double distanceSquared = pos.distSqr(minecraft.player.blockPosition());

            if (distanceSquared > (double) distance * distance) {
                callback.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }
}