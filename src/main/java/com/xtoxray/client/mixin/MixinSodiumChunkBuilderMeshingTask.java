package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Совместимость с Embeddium 1.21.1.
 *
 * Embeddium использует собственный WorldSlice при построении мешей чанков
 * и поэтому не проходит через vanilla RenderChunkRegion.
 */
@Mixin(targets = "org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask")
public abstract class MixinEmbeddiumChunkBuilderMeshingTask {

    @Redirect(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lorg/embeddedt/embeddium/impl/world/WorldSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private BlockState xtoxray$filterWorldSlice(
            @Coerce Object worldSlice,
            int x,
            int y,
            int z
    ) {
        BlockState state = ((BlockAndTintGetter) worldSlice).getBlockState(new BlockPos(x, y, z));
        XrayState xray = XrayState.getInstance();

        if (!xray.isActive() || state.isAir()) {
            return state;
        }

        if (!xray.shouldRender(state)) {
            return Blocks.AIR.defaultBlockState();
        }

        int distance = xray.getOreRenderDistance();
        if (distance > 0) {
            double dx = x;
            double dy = y;
            double dz = z;

            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                double distanceSquared = minecraft.player.blockPosition().distSqr(
                    new BlockPos((int) dx, (int) dy, (int) dz)
                );

                if (distanceSquared > (double) distance * distance) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }

        return state;
    }
}
