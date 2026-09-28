package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Совместимость с Sodium 0.8.13 для Minecraft 1.21.1.
 *
 * Фильтруем LevelSlice на самом уровне getBlockState(), а не только
 * прямой вызов из ChunkBuilderMeshingTask. Поэтому Sodium использует
 * отфильтрованное состояние и для соседей при расчёте видимых граней.
 */
@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice",
    remap = false
)
public abstract class MixinSodiumChunkBuilderMeshingTask {

    @Inject(
        method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void xtoxray$filterLevelSlice(
            int x,
            int y,
            int z,
            CallbackInfoReturnable<BlockState> cir
    ) {
        XrayState xray = XrayState.getInstance();
        if (!xray.isActive()) {
            return;
        }

        BlockState state = cir.getReturnValue();
        if (state.isAir()) {
            return;
        }

        if (!xray.shouldRender(state)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
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
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }
}
