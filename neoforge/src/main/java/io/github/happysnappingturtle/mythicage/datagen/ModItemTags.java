package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** 物品标签：来自 {@link Content} 中各条目声明的物品标签。 */
public final class ModItemTags extends ItemTagsProvider {
    public ModItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                       CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, MythicAgeIds.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (ContentEntry e : Content.entries()) {
            Item item = e.item().get();
            for (TagKey<Item> tag : e.itemTags()) {
                tag(tag).add(item);
            }
        }
    }
}
