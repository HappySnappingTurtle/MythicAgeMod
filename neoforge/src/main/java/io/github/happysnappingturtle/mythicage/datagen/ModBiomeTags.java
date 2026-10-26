package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.content.greek.worldgen.GreekWorldGen;
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
        tag(GreekWorldGen.OLIVE_COMMON_BIOMES).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
        tag(GreekWorldGen.OLIVE_SPARSE_BIOMES).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
                Biomes.WINDSWEPT_FOREST);
        // 野葡萄藤：希腊主群系与繁花森林
        // 柏树：丘陵、草甸、平原（地中海柏树长在干燥的山坡）；月桂：繁花森林、风袭森林、草甸（较湿润的林地、沟谷）
        tag(GreekWorldGen.CYPRESS_BIOMES).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.MEADOW, Biomes.PLAINS);
        tag(GreekWorldGen.LAUREL_BIOMES).add(Biomes.FLOWER_FOREST, Biomes.WINDSWEPT_FOREST, Biomes.MEADOW);
        // 希腊神话子区（T15，01_希腊 §2）：只用于野外生物与小结构的生成规则
        tag(ModTags.subregion(Civilization.GREEK, "arcadia")).add(Biomes.MEADOW, Biomes.FLOWER_FOREST, Biomes.PLAINS,
                Biomes.SUNFLOWER_PLAINS);
        tag(ModTags.subregion(Civilization.GREEK, "lerna")).add(Biomes.SWAMP);
        tag(ModTags.subregion(Civilization.GREEK, "aegean")).add(Biomes.STONY_SHORE);
        tag(ModTags.subregion(Civilization.GREEK, "highlands")).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
                Biomes.WINDSWEPT_FOREST);
        tag(ModTags.subregion(Civilization.GREEK, "olympus")).add(Biomes.JAGGED_PEAKS);
        // 半人马营地：阿卡迪亚与风袭森林（09 合同 ST-GR-11-01）
        tag(GreekWorldGen.CENTAUR_CAMP_BIOMES).addTag(ModTags.subregion(Civilization.GREEK, "arcadia")).add(Biomes.WINDSWEPT_FOREST);
        // 树仙女圣林：繁花森林与草甸（01_希腊 W5）
        tag(GreekWorldGen.HAMADRYAD_GROVE_BIOMES).add(Biomes.FLOWER_FOREST, Biomes.MEADOW);
        // 卡尔基诺斯：勒拿沼泽与爱琴海石岸的浅滩（01_希腊 W4）
        tag(GreekWorldGen.KARKINOS_BIOMES).addTag(ModTags.subregion(Civilization.GREEK, "lerna"))
                .addTag(ModTags.subregion(Civilization.GREEK, "aegean"));
        // 哈耳庇厄崖巢：风暴高地的崖壁与爱琴海岸的石岸（01_希腊 W2）
        tag(GreekWorldGen.HARPY_NEST_BIOMES).addTag(ModTags.subregion(Civilization.GREEK, "highlands"))
                .addTag(ModTags.subregion(Civilization.GREEK, "aegean"));
        tag(ModTags.subregion(Civilization.GREEK, "nemea")).add(Biomes.SAVANNA);
        tag(GreekWorldGen.GRAPEVINE_BIOMES).addTag(ModTags.region(Civilization.GREEK)).add(Biomes.FLOWER_FOREST);
    }
}
