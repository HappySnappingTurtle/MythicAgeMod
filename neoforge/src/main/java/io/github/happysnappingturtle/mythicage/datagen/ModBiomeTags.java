package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.worldgen.ModWorldGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * 文明地域群系标签（总纲 §4.1）。只用原版群系；地形模组可以用数据包往标签里加群系。
 * T6 只建立希腊的两个标签，其余文明在各自阶段补齐。
 */
public final class ModBiomeTags extends BiomeTagsProvider {
    public ModBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, MythicAgeIds.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.region(Civilization.GREEK)).add(Biomes.MEADOW, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS,
                Biomes.STONY_SHORE, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.SWAMP);
        tag(ModTags.regionSecondary(Civilization.GREEK)).add(Biomes.FLOWER_FOREST, Biomes.SAVANNA,
                Biomes.JAGGED_PEAKS);
        // 橄榄树：平原、草甸较多，风袭丘陵较少（T6b 设计文档 §3.3）
        tag(ModWorldGen.OLIVE_COMMON_BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
        tag(ModWorldGen.OLIVE_SPARSE_BIOMES).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
                Biomes.WINDSWEPT_FOREST);
        // 野葡萄藤：希腊主群系与繁花森林
        // 柏树：丘陵、草甸、平原（地中海柏树长在干燥的山坡）；月桂：繁花森林、风袭森林、草甸（较湿润的林地、沟谷）
        tag(ModWorldGen.CYPRESS_BIOMES).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.MEADOW, Biomes.PLAINS);
        tag(ModWorldGen.LAUREL_BIOMES).add(Biomes.FLOWER_FOREST, Biomes.WINDSWEPT_FOREST, Biomes.MEADOW);
        tag(ModWorldGen.GRAPEVINE_BIOMES).addTag(ModTags.region(Civilization.GREEK)).add(Biomes.FLOWER_FOREST);
    }
}
