package io.github.happysnappingturtle.mythicage.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * 方块模型种类（datagen 据此生成方块状态与模型）。贴图默认取 {@code mythic_age:block/<id>}，
 * 可用 {@code Content.BlockBuilder#texture} 改为别的贴图（例如 {@link Recolor} 换色贴图）；
 * 台阶、楼梯、墙使用其 base 方块的贴图。
 */
public sealed interface BlockModelSpec {
    /** 六面同一张贴图。 */
    record CubeAll() implements BlockModelSpec {
    }

    /**
     * 台阶，贴图取自 base 方块。
     *
     * @param side 可选：台阶侧面的专用贴图（例如表现两块石板叠缝），为 null 时侧面也用 base 的贴图
     */
    record Slab(Supplier<? extends Block> base, ResourceLocation side) implements BlockModelSpec {
    }

    /** 楼梯，贴图取自 base 方块。 */
    record Stairs(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 墙，贴图取自 base 方块。 */
    record Wall(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 侧面一张贴图（{@code block/<id>}），顶面和底面另用 end 贴图，例如饰带：侧面是回纹，上下是平滑大理石。 */
    record Column(ResourceLocation end) implements BlockModelSpec {
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

    static BlockModelSpec slab(Supplier<? extends Block> base) {
        return new Slab(base, null);
    }

    static BlockModelSpec slab(Supplier<? extends Block> base, ResourceLocation side) {
        return new Slab(base, side);
    }

    static BlockModelSpec column(ResourceLocation end) {
        return new Column(end);
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
