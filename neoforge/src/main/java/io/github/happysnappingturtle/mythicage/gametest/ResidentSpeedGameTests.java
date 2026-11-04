package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.WalkSpeed;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierBrain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GA-1 居民移动速度（TC-GA1-10～14）：平直石道上实测寻路倍率对应的米/秒。
 * 实体直接生成、清掉自带目标，只测倍率到实际速度的换算；工作单、士兵指令用哪个倍率由常量保证。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResidentSpeedGameTests {
    private static final String BATCH = "resident_speed";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    /** 跳过起步加速，从第 START 刻量到第 END 刻。 */
    private static final int START = 13, END = 43;
    private static final double TOLERANCE = 0.25;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private ResidentSpeedGameTests() {
    }

    /** TC-GA1-10 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void resident_walk_speed(GameTestHelper h) {
        measure(h, ModEntities.RESIDENT.get(), ResidentEntity.WALK_SPEED, WalkSpeed.WALK, "居民走路");
    }

    /** TC-GA1-11 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void resident_run_speed(GameTestHelper h) {
        measure(h, ModEntities.RESIDENT.get(), ResidentEntity.RUN_SPEED, WalkSpeed.RUN, "居民跑步");
    }

    /** TC-GA1-12 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void resident_flee_speed(GameTestHelper h) {
        measure(h, ModEntities.RESIDENT.get(), ResidentEntity.FLEE_SPEED, WalkSpeed.FLEE, "居民逃跑");
    }

    /** TC-GA1-13 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void soldier_run_speed(GameTestHelper h) {
        measure(h, ModEntities.RESIDENT.get(), SoldierBrain.RUN, WalkSpeed.SOLDIER_RUN, "士兵跑");
    }

    /** TC-GA1-14 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void guardian_errand_speed(GameTestHelper h) {
        measure(h, ModEntities.GREEK_GUARDIAN.get(), GuardianEntity.ERRAND_SPEED, WalkSpeed.ERRAND, "祭司跑腿");
    }

    /** 铺 1×34 的石道、两侧屏障，实体从一端寻路到另一端，量水平位移换算米/秒。 */
    private static void measure(GameTestHelper h, EntityType<? extends PathfinderMob> type, double modifier,
                                double expected, String what) {
        int x = 8;
        for (int z = 1; z <= 34; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            for (int y = 2; y <= 4; y++) {
                h.setBlock(new BlockPos(x - 1, y, z), Blocks.BARRIER);
                h.setBlock(new BlockPos(x + 1, y, z), Blocks.BARRIER);
            }
        }
        PathfinderMob mob = h.spawn(type, new Vec3(x + 0.5, 2, 1.5));
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);
        // 终点不超出寻路范围（祭司只有 16 格）
        int endZ = 1 + Math.min(33, (int) mob.getAttributeValue(Attributes.FOLLOW_RANGE) - 2);
        BlockPos end = h.absolutePos(new BlockPos(x, 2, endZ));
        Vec3[] from = new Vec3[1];
        // 等落地再寻路：地面寻路要求站在地上
        h.runAtTickTime(3, () -> h.assertTrue(mob.getNavigation().moveTo(end.getX() + 0.5, end.getY(), end.getZ() + 0.5, modifier),
                what + "：寻不到路（onGround=" + mob.onGround() + "）"));
        h.runAtTickTime(START, () -> from[0] = mob.position());
        h.runAtTickTime(END, () -> {
            Vec3 d = mob.position().subtract(from[0]);
            double mps = Math.sqrt(d.x * d.x + d.z * d.z) / ((END - START) / 20.0);
            LOGGER.info("GA-1 {} 实测 {} 米/秒（目标 {}）", what, String.format("%.2f", mps), expected);
            h.assertTrue(Math.abs(mps - expected) <= expected * TOLERANCE,
                    String.format("%s：实测 %.2f 米/秒，目标 %.1f（±%d%%）", what, mps, expected, (int) (TOLERANCE * 100)));
            h.succeed();
        });
    }
}
