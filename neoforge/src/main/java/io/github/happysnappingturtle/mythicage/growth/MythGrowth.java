package io.github.happysnappingturtle.mythicage.growth;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.growth.GrowthRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * 统一生长入口（T6b 设计文档 §10.2）：本模组的果树、作物只通过这里推进，
 * 先发出 NeoForge 的 CropGrowEvent.Pre（季节类模组可以拦截），推进后发出 Post。
 * 以后的神力（POWER）与村庄逻辑（VILLAGE）也走这里，便于统一加上限。
 */
public final class MythGrowth {
    public enum Source { RANDOM_TICK, BONE_MEAL, POWER, VILLAGE }

    private MythGrowth() {
    }

    public static WorldRulesConfig.Growth config() {
        return ModConfigs.worldRules().current().greek().growth();
    }

    /** 随机刻时按期望时长换算的几率决定是否推进。 */
    public static boolean rollRandomTick(RandomSource random, long expectedTicksPerStage) {
        return random.nextDouble() < GrowthRules.chancePerRandomTick(expectedTicksPerStage);
    }

    /**
     * 尝试把 from 推进为 to。
     *
     * @return 是否推进（被其他模组拦截时为 false）
     */
    public static boolean tryAdvance(ServerLevel level, BlockPos pos, BlockState from, BlockState to, Source source) {
        if (!CommonHooks.canCropGrow(level, pos, from, true)) {
            return false;
        }
        level.setBlock(pos, to, 2);
        CommonHooks.fireCropGrowPost(level, pos, from);
        return true;
    }
}
