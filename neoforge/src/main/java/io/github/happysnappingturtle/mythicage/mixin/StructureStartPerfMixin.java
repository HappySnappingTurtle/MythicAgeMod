package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.perf.Perf;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 仅对本模组结构落块计时；同区块的全部结构件统一累计，支持世界生成工作线程。 */
@Mixin(StructureStart.class)
public abstract class StructureStartPerfMixin {
    @Shadow @Final private Structure structure;
    // 静态：每个 StructureStart 都带一个 ThreadLocal 太浪费；同一线程里 HEAD 与 RETURN 成对出现
    @Unique private static final ThreadLocal<Perf.ChunkSample> mythicAge$placement = new ThreadLocal<>();

    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void mythicAge$begin(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                                 RandomSource random, BoundingBox box, ChunkPos pos, CallbackInfo ci) {
        if (!Perf.running()) {
            return;
        }
        var id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        if (id != null && id.getNamespace().equals(MythicAgeIds.MOD_ID)) {
            mythicAge$placement.set(Perf.beginChunk(generator, pos));
        }
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void mythicAge$end(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                               RandomSource random, BoundingBox box, ChunkPos pos, CallbackInfo ci) {
        Perf.ChunkSample sample = mythicAge$placement.get();
        mythicAge$placement.remove();
        if (sample != null) {
            sample.finish();
        }
    }
}
