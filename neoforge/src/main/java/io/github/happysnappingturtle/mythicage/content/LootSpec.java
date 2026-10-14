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

    static LootSpec none() {
        return new None();
    }
}
