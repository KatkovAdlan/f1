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
 * Совместимость с Sodium 0.8.13 для Minecraft 1.21.1.
 *
 * Sodium строит меши чанков через собственный LevelSlice, поэтому
 * vanilla RenderChunkRegion здесь не участвует.
 */
@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask",
    remap = false
)
public abstract class MixinSodiumChunkBuilderMeshingTask {

    @Redirect(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            remap = false
        ),
        remap = false
    )
    private BlockState xtoxray$filterLevelSlice(
            @Coerce Object levelSlice,
            int x,
            int y,
            int z
    ) {
        BlockState state = ((BlockAndTintGetter) levelSlice).getBlockState(new BlockPos(x, y, z));
        XrayState xray = XrayState.getInstance();

        if (!xray.isActive() || state.isAir()) {
            return state;
        }

        if (!xray.shouldRender(state)) {
            return Blocks.AIR.defaultBlockState();
        }

        int distance = xray.getOreRenderDistance();
        if (distance > 0) {
            int cx = xray.getRenderCenterX();
            int cy = xray.getRenderCenterY();
            int cz = xray.getRenderCenterZ();

            long dx = (long) x - cx;
            long dy = (long) y - cy;
            long dz = (long) z - cz;

            if (dx * dx + dy * dy + dz * dz > (long) distance * distance) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return state;
    }
}
