package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/** 方块战利品：按 {@link LootSpec} 生成。 */
public final class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (ContentEntry e : Content.entries()) {
            if (e.block().isEmpty()) {
                continue;
            }
            Block block = e.block().get().get();
            switch (e.loot().orElseThrow()) {
                case LootSpec.Self s -> dropSelf(block);
                case LootSpec.Ore o -> add(block, createOreDrop(block, o.drop().get().asItem()));
                case LootSpec.Slab s -> add(block, createSlabItemTable(block));
                case LootSpec.None n -> add(block, noDrop());
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Content.entries().stream().filter(ContentEntry::isBlock)
                .<Block>map(e -> e.block().orElseThrow().get()).toList();
    }
}
