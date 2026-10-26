package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.structure.NearSpawnRings;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 带 {@code mythic_age:near_spawn_rings} 标签的同心环结构集：原版算完环位置后平移到出生点、再挪到偏好的陆地上（{@link NearSpawnRings}，T22 §5.5）。 */
@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @Shadow
    @Final
    private BiomeSource biomeSource;

    @Inject(method = "generateRingPositions", at = @At("RETURN"), cancellable = true)
    private void mythicAge$landRings(Holder<StructureSet> set, ConcentricRingsStructurePlacement placement,
                                     CallbackInfoReturnable<CompletableFuture<List<ChunkPos>>> cir) {
        if (!set.is(NearSpawnRings.TAG)) {
            return;
        }
        BiomeSource source = biomeSource;
        RandomState random = randomState;
        cir.setReturnValue(cir.getReturnValue().thenApplyAsync(
                ring -> NearSpawnRings.adjust(ring, placement.preferredBiomes(), source, random.sampler()), Util.backgroundExecutor()));
    }
}
