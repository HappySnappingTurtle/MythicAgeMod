package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

/** 方块战利品：按 {@link LootSpec} 生成。自定义战利品通过本类公开的辅助方法组合原版写法。 */
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
                case LootSpec.Door d -> add(block, createDoorTable(block));
                case LootSpec.Other o -> dropOther(block, o.item().get());
                case LootSpec.Potted p -> add(block, createPotFlowerItemTable(p.plant().get()));
                case LootSpec.Custom c -> add(block, c.table().apply(this));
                case LootSpec.None n -> add(block, noDrop());
            }
        }
    }

    /** 原版树叶掉落（树苗几率、木棍），供自定义战利品组合。 */
    public LootTable.Builder leaves(Block leaves, Block sapling) {
        return createLeavesDrops(leaves, sapling, NORMAL_LEAVES_SAPLING_CHANCES);
    }

    /** 剪刀或精准采集得到本体，否则掉 drop（数量为 min–max 的均匀随机）。 */
    public LootTable.Builder shearsOr(Block block, ItemLike drop, float min, float max) {
        return createShearsDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))));
    }

    /** 结果树叶：原版树叶掉落，另在 property = ripe 时多掉 1 个果实。 */
    public LootTable.Builder fruitLeaves(Block leaves, Block sapling, IntegerProperty property, int ripe, ItemLike fruit) {
        return leaves(leaves, sapling).withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(fruit))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(property, ripe))));
    }

    /**
     * 作物架（多格时只由 part = bottom 的一格掉落）：总是掉架子本身；已种下（stage ≠ 0）时掉 1 根插条；
     * 成熟（stage = mature）时另掉 min–max 个果实。
     */
    public LootTable.Builder trellis(Block block, Property<?> part, StringRepresentable bottom, IntegerProperty stage,
                                     int mature, ItemLike cutting, ItemLike fruit, float min, float max) {
        var isBottom = state(block, StatePropertiesPredicate.Builder.properties()
                .hasProperty(part, bottom.getSerializedName()));
        return LootTable.lootTable()
                .withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block)).when(isBottom)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(cutting))
                        .when(isBottom)
                        .when(state(block, StatePropertiesPredicate.Builder.properties().hasProperty(stage, 0)).invert()))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(fruit).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))))
                        .when(isBottom)
                        .when(state(block, StatePropertiesPredicate.Builder.properties().hasProperty(stage, mature))));
    }

    private static LootItemBlockStatePropertyCondition.Builder state(Block block, StatePropertiesPredicate.Builder props) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(props);
    }

    /** 灌木：剪刀得到本体；否则掉插条（a 个）与果实（b 个）。 */
    public LootTable.Builder bush(Block block, ItemLike cutting, float cMin, float cMax, ItemLike fruit, float fMin, float fMax) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(block).when(HAS_SHEARS.or(hasSilkTouch())),
                                applyExplosionDecay(block, LootItem.lootTableItem(cutting)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(cMin, cMax)))))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(applyExplosionDecay(block, LootItem.lootTableItem(fruit)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(fMin, fMax)))))
                        .when(HAS_SHEARS.or(hasSilkTouch()).invert()));
    }

    public HolderLookup.Provider lookup() {
        return registries;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Content.entries().stream().filter(ContentEntry::isBlock)
                .<Block>map(e -> e.block().orElseThrow().get()).toList();
    }
}
