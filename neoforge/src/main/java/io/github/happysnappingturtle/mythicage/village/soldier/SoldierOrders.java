package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * 调度器每次规划时发给一名士兵的命令（T20 设计文档 §5，不存档）：班次状态、岗位、兵装、要不要去换装。
 *
 * @param posts    本村所有岗位（拴绳与巡逻用）
 * @param post     此刻该去的岗位序号；不在岗为 -1
 * @param keeper   换装、补弹药的地方（谷仓管事位；没有就是村钟）
 * @param needSwap 兵装耐久用完了，不在战斗时去换装
 * @param issuedAt 发令的世界刻（太久没更新就作废，比如村庄停止运行）
 */
public record SoldierOrders(UUID village, String profession, SoldierProfile profile, SoldierProfile.Tier tier,
                            DutyRules.State state, List<BlockPos> posts, int post, BlockPos keeper, boolean needSwap,
                            BlockPos bell, long issuedAt) {
    /** 超过这么久没有新命令就作废（刻）。 */
    public static final long STALE = 200;

    public SoldierOrders {
        posts = List.copyOf(posts);
    }

    public BlockPos postPos() {
        return post >= 0 && post < posts.size() ? posts.get(post) : null;
    }
}
