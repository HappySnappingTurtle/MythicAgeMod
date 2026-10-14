package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Recolor;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.worldgen.ModWorldGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * datagen 入口（T6 设计文档 §4）：{@code ./gradlew :neoforge:runData}，输出到
 * {@code neoforge/src/generated/resources/}（入 git）。全部数据都从 {@code Content} 与各文明的配方类生成。
 */
public final class ModDataGen {
    public static final List<String> LOCALES = List.of("zh_cn", "en_us", "ja_jp");

    private ModDataGen() {
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        // 换色贴图在游戏加载资源时生成，磁盘上没有文件：告诉模型检查器它们存在
        for (ResourceLocation sprite : Recolor.sprites()) {
            files.trackGenerated(sprite, PackType.CLIENT_RESOURCES, ".png", "textures");
        }

        DatapackBuiltinEntriesProvider worldgen = new DatapackBuiltinEntriesProvider(out, lookup,
                ModWorldGen.builder(), Set.of(MythicAgeIds.MOD_ID));
        gen.addProvider(event.includeServer(), worldgen);
        CompletableFuture<HolderLookup.Provider> full = worldgen.getRegistryProvider();

        gen.addProvider(event.includeClient(), new ModBlockStates(out, files));
        gen.addProvider(event.includeClient(), new ModItemModels(out, files));
        for (String locale : LOCALES) {
            gen.addProvider(event.includeClient(), new ModLang(out, locale));
        }
        gen.addProvider(event.includeServer(), new ModRecipeProvider(out, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        ModBlockTags blockTags = gen.addProvider(event.includeServer(), new ModBlockTags(out, lookup, files));
        gen.addProvider(event.includeServer(), new ModItemTags(out, lookup, blockTags.contentsGetter(), files));
        gen.addProvider(event.includeServer(), new ModBiomeTags(out, full, files));
    }
}
