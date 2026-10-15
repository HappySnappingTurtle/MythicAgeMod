package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** 方块标签：来自 {@link Content} 中各条目声明的标签。 */
public final class ModBlockTags extends BlockTagsProvider {
    public ModBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, MythicAgeIds.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (ContentEntry e : Content.entries()) {
            if (e.block().isEmpty()) {
                continue;
            }
            Block block = e.block().get().get();
            for (TagKey<Block> tag : e.blockTags()) {
                tag(tag).add(block);
            }
        }
        for (GreekTemple.ExtraTag extra : GreekTemple.extraTags()) {
            var appender = tag(extra.tag());
            extra.blocks().forEach(b -> appender.add(b.get()));
            extra.tags().forEach(appender::addTag);
        }
    }
}
