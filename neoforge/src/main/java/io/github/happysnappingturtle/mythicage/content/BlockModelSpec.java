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

    /** 原木（三向轴）：侧面与端面贴图。 */
    record Log(ResourceLocation side, ResourceLocation end) implements BlockModelSpec {
    }

    /** 栅栏、栅栏门、按钮、压力板：贴图取自 base 方块（通常是木板）。 */
    record Fence(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    record FenceGate(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    record Button(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    record PressurePlate(Supplier<? extends Block> base) implements BlockModelSpec {
    }

    /** 门：上下两半各一张贴图（cutout）。 */
    record Door(ResourceLocation bottom, ResourceLocation top) implements BlockModelSpec {
    }

    /** 活板门（可朝向，cutout）。 */
    record Trapdoor(ResourceLocation texture) implements BlockModelSpec {
    }

    /**
     * 树叶：底层树叶贴图，另按整数属性的每个取值叠加一层（值 0 不叠加）。例如橄榄树叶的 fruit 0/1/2。
     *
     * @param overlays 下标为属性取值，0 号位置必须为 null
     */
    record Leaves(ResourceLocation texture, net.minecraft.world.level.block.state.properties.IntegerProperty property,
                  java.util.List<ResourceLocation> overlays) implements BlockModelSpec {
    }

    /** 十字形（树苗、藤类，cutout）。 */
    record Cross(ResourceLocation texture) implements BlockModelSpec {
    }

    /** 盆栽：原版花盆 + 植物贴图。 */
    record Potted(ResourceLocation plant) implements BlockModelSpec {
    }

    /** 立式告示牌；同时为对应的墙上告示牌生成方块状态。 */
    record Sign(ResourceLocation particle, Supplier<? extends Block> wall) implements BlockModelSpec {
    }

    /** 悬挂告示牌；同时为对应的墙上悬挂告示牌生成方块状态。 */
    record HangingSign(ResourceLocation particle, Supplier<? extends Block> wall) implements BlockModelSpec {
    }

    /** 由另一个条目一并生成方块状态（墙上告示牌等），本条目不单独生成。 */
    record GeneratedBy(String ownerId) implements BlockModelSpec {
    }

    /** 葡萄架（三格）：每个 part × stage 一个模型（模板 template_trellis + block/<id>[_stage<n>]_<part>），按 axis 旋转。 */
    record Trellis() implements BlockModelSpec {
    }

    /**
     * 有朝向、带两种外观的手写模型方块：{@code block/<id>} 与 {@code block/<id>_<on>}，
     * 由布尔属性切换（例如压榨器的 pressing），按 facing 旋转。
     */
    record FacingToggle(net.minecraft.world.level.block.state.properties.BooleanProperty toggle, String onSuffix)
            implements BlockModelSpec {
    }

    /** 有朝向的手写模型方块（block/<id>），按 facing 旋转。 */
    record FacingManual() implements BlockModelSpec {
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

    static BlockModelSpec cross(ResourceLocation texture) {
        return new Cross(texture);
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
