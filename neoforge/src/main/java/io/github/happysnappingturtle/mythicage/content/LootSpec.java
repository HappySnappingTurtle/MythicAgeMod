package io.github.happysnappingturtle.mythicage.content;

import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/** 方块战利品种类（datagen 据此生成战利品表）。 */
public sealed interface LootSpec {
    /** 掉落本体。 */
    record Self() implements LootSpec {
    }

    /** 矿石：精准采集掉本体，否则掉 drop（受时运影响，与原版铁矿相同）。 */
    record Ore(Supplier<? extends ItemLike> drop) implements LootSpec {
    }

    /** 台阶：双层时掉 2 个。 */
    record Slab() implements LootSpec {
    }

    /** 门：只在下半掉落。 */
    record Door() implements LootSpec {
    }

    /** 掉落另一种物品（墙上告示牌掉告示牌物品等）。 */
    record Other(Supplier<? extends ItemLike> item) implements LootSpec {
    }

    /** 盆栽：掉花盆与植物。 */
    record Potted(Supplier<? extends ItemLike> plant) implements LootSpec {
    }

    /** 自定义：由 datagen 中的函数生成战利品表（树叶、作物等）。 */
    record Custom(java.util.function.Function<io.github.happysnappingturtle.mythicage.datagen.ModBlockLoot,
            net.minecraft.world.level.storage.loot.LootTable.Builder> table) implements LootSpec {
    }

    /** 不掉落（必须显式声明）。 */
    record None() implements LootSpec {
    }

    static LootSpec self() {
        return new Self();
    }

    static LootSpec ore(Supplier<? extends ItemLike> drop) {
        return new Ore(drop);
    }

    static LootSpec slab() {
        return new Slab();
    }

    static LootSpec door() {
        return new Door();
    }

    static LootSpec other(Supplier<? extends ItemLike> item) {
        return new Other(item);
    }

    static LootSpec potted(Supplier<? extends ItemLike> plant) {
        return new Potted(plant);
    }

    static LootSpec custom(java.util.function.Function<io.github.happysnappingturtle.mythicage.datagen.ModBlockLoot,
            net.minecraft.world.level.storage.loot.LootTable.Builder> table) {
        return new Custom(table);
    }

    static LootSpec none() {
        return new None();
    }
}
