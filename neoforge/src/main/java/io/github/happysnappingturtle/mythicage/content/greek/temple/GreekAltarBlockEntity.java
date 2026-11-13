package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 旧祭坛的方块实体（原 T8a 奥林匹斯祭坛）：GA-13 起只用来读出旧存档里的主人与供奉的圣物，加载时迁移
 * （{@link LegacyAltars}）。原来的扫描结果、祈祷位、近 7 日信徒、献供日不再读写（旧存档里的这些键读档时忽略）。
 */
public class GreekAltarBlockEntity extends ShrineBlockEntity {
    public GreekAltarBlockEntity(BlockPos pos, BlockState state) {
        super(GreekTemple.GREEK_ALTAR_BE.get(), pos, state);
    }

    @Override
    public ShrineKind kind() {
        return GreekTemple.ALTAR_KIND;
    }
}
