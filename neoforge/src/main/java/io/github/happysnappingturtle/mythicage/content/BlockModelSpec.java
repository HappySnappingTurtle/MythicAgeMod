package io.github.happysnappingturtle.mythicage.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * 方块模型种类（datagen 据此生成方块状态与模型）。贴图默认取 {@code mythic_age:block/<id>}。
 */
public sealed interface BlockModelSpec {
    /** 六面同一张贴图。 */
    record CubeAll() implements BlockModelSpec {
    }

    /** 矿石：底层用原版方块贴图（如 minecraft:block/stone），上层叠加 {@code block/<id>_overlay}。 */
    record OreOverlay(ResourceLocation baseTexture) implements BlockModelSpec {
    }

    /** 台阶，贴图取自 base 方块。 */
    record Slab(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 楼梯，贴图取自 base 方块。 */
    record Stairs(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 墙，贴图取自 base 方块。 */
    record Wall(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 柱状（原木式），侧面 {@code block/<id>}，端面 {@code block/<id>_top}。 */
    record Pillar() implements BlockModelSpec {
    }

    /** 模型手写在资源目录中，datagen 不生成。 */
    record Manual() implements BlockModelSpec {
    }

    static BlockModelSpec cubeAll() {
        return new CubeAll();
    }

    static BlockModelSpec oreOverlay(ResourceLocation baseTexture) {
        return new OreOverlay(baseTexture);
    }

    static BlockModelSpec slab(Supplier<? extends Block> base) {
        return new Slab(base);
    }

    static BlockModelSpec stairs(Supplier<? extends Block> base) {
        return new Stairs(base);
    }

    static BlockModelSpec wall(Supplier<? extends Block> base) {
        return new Wall(base);
    }

    static BlockModelSpec pillar() {
        return new Pillar();
    }

    static BlockModelSpec manual() {
        return new Manual();
    }
}
