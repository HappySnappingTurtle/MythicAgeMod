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
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Predicate;

/** 结构规划也计入；不能只统计模板落块，漏掉村庄布局和迷宫布局。 */
@Mixin(Structure.class)
public abstract class StructurePerfMixin {
    // 静态：同一线程里 HEAD 与 RETURN 成对出现，不用每个结构实例各带一个
    @Unique private static final ThreadLocal<Perf.ChunkSample> mythicAge$planning = new ThreadLocal<>();

    @Inject(method = "generate", at = @At("HEAD"))
    private void mythicAge$begin(RegistryAccess registry, ChunkGenerator generator, BiomeSource biomes,
                                 RandomState random, StructureTemplateManager templates, long seed, ChunkPos pos,
                                 int references, LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome,
                                 CallbackInfoReturnable<StructureStart> cir) {
        if (!Perf.running()) {
            return;
        }
        var id = registry.registryOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);
        if (id != null && id.getNamespace().equals(MythicAgeIds.MOD_ID)) {
            mythicAge$planning.set(Perf.beginChunk(generator, pos));
        }
    }

    @Inject(method = "generate", at = @At("RETURN"))
    private void mythicAge$end(RegistryAccess registry, ChunkGenerator generator, BiomeSource biomes,
                               RandomState random, StructureTemplateManager templates, long seed, ChunkPos pos,
                               int references, LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome,
                               CallbackInfoReturnable<StructureStart> cir) {
        Perf.ChunkSample sample = mythicAge$planning.get();
        mythicAge$planning.remove();
        if (sample != null) {
            sample.finish();
        }
    }
}
