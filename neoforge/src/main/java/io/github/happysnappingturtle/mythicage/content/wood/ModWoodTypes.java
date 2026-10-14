package io.github.happysnappingturtle.mythicage.content.wood;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 本模组的木材类型（T6b 设计文档 §7.2）。
 * <p>
 * 告示牌贴图采用「原版造型换色」（signs 图集里的 paletted_permutations），换色贴图名固定为
 * {@code minecraft:entity/signs/oak_mythic_age_<id>}。原版按木材类型名定位告示牌贴图
 * （{@code Sheets.createSignMaterial}：名字的命名空间 + entity/signs/ + 路径），所以这里把木材类型名设为
 * {@code minecraft:oak_mythic_age_olive}，与换色贴图名对齐；模组里因此不需要放任何原版像素。
 * 悬挂告示牌编辑界面的背景同理位于 {@code minecraft:textures/gui/hanging_signs/oak_mythic_age_olive.png}（自绘）。
 */
public final class ModWoodTypes {
    public static final BlockSetType OLIVE_SET = BlockSetType.register(new BlockSetType("mythic_age:olive"));
    public static final WoodType OLIVE = WoodType.register(new WoodType("minecraft:oak_mythic_age_olive", OLIVE_SET));

    private ModWoodTypes() {
    }

    public static void init() {
    }
}
