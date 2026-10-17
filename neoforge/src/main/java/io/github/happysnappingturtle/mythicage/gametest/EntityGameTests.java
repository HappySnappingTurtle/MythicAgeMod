package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline;
import io.github.happysnappingturtle.mythicage.core.rig.RigDefinition;
import io.github.happysnappingturtle.mythicage.entity.MinotaurEntity;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.rig.RigRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** T7 弥诺陶洛斯样板外壳的集成测试（TC-T7-40～45）。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityGameTests {
    private static final String BATCH = "entity_minotaur";
    private static final ResourceLocation MINOTAUR_ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "minotaur");
    private static final double EPS = 1e-3;

    private EntityGameTests() {
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static MinotaurEntity spawn(GameTestHelper helper, double x, double z) {
        MinotaurEntity e = helper.spawn(ModEntities.MINOTAUR.get(), new Vec3(x, 1, z));
        e.setYRot(0);
        e.setYBodyRot(0);
        e.setYHeadRot(0);
        return e;
    }

    /** TC-T7-40：实体类型已注册，尺寸 1.1×2.5。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void minotaur_registered(GameTestHelper helper) {
        floor(helper);
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(MINOTAUR_ID) == ModEntities.MINOTAUR.get(), "实体类型未注册");
        MinotaurEntity e = spawn(helper, 5.5, 5.5);
        helper.assertTrue(Math.abs(e.getBbWidth() - 1.1) < EPS && Math.abs(e.getBbHeight() - 2.5) < EPS,
                "尺寸应为 1.1×2.5，实际 " + e.getBbWidth() + "×" + e.getBbHeight());
        helper.succeed();
    }

    /** TC-T7-41：Boss 变体尺寸 1.54×3.5，能放进 3 格宽、5 格高的通道。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void boss_variant_dimensions(GameTestHelper helper) {
        floor(helper);
        for (int y = 1; y <= 6; y++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(3, y, z, Blocks.STONE);
                helper.setBlock(7, y, z, Blocks.STONE);
            }
        }
        for (int x = 4; x <= 6; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(x, 6, z, Blocks.STONE);
            }
        }
        MinotaurEntity e = spawn(helper, 5.5, 5.5);
        e.setVariant("boss");
        helper.assertTrue("boss".equals(e.variant()) && Math.abs(e.variantScale() - 1.4F) < EPS, "变体应为 boss、缩放 1.4");
        helper.assertTrue(Math.abs(e.getBbWidth() - 1.54) < EPS && Math.abs(e.getBbHeight() - 3.5) < EPS,
                "Boss 尺寸应为 1.54×3.5，实际 " + e.getBbWidth() + "×" + e.getBbHeight());
        helper.assertTrue(helper.getLevel().noCollision(e, e.getBoundingBox()), "Boss 碰撞箱不应卡进 3×5 通道");
        e.setVariant("");
        helper.assertTrue(Math.abs(e.getBbHeight() - 2.5) < EPS, "回到基础外观后尺寸应恢复");
        helper.succeed();
    }

    /** TC-T7-42：服务端骨架数据已加载。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void rig_loaded(GameTestHelper helper) {
        RigDefinition rig = RigRegistry.get(MINOTAUR_ID).orElse(null);
        helper.assertTrue(rig != null, "缺少 mythic_age:minotaur 的骨架数据");
        helper.assertTrue(rig.locators().size() == 4, "定位点应为 4 个，实际 " + rig.locators().size());
        helper.assertTrue(Math.abs(rig.width() - ModEntities.MINOTAUR.get().getWidth()) < EPS
                && Math.abs(rig.height() - ModEntities.MINOTAUR.get().getHeight()) < EPS, "骨架碰撞与实体类型尺寸不一致");
        helper.succeed();
    }

    private record Sample(ChargeTimeline.Phase phase, MythAnimState state, Vec3 pos) {
    }

    /**
     * 第 {@code startAt} 刻开始冲锋，此后每刻采样；第 {@code checkAt} 刻执行断言。
     * 所有回调都在测试开始时注册：在回调里再注册回调会修改 GameTest 正在遍历的表。
     */
    private static void chargeScenario(GameTestHelper helper, MinotaurEntity e, double distance, int startAt, int checkAt,
                                       java.util.function.BiConsumer<Vec3, List<Sample>> check) {
        List<Sample> samples = new ArrayList<>();
        Vec3[] start = new Vec3[1];
        helper.runAfterDelay(startAt, () -> {
            start[0] = e.position();
            e.startCharge(distance);
        });
        helper.onEachTick(() -> {
            if (start[0] != null) {
                samples.add(new Sample(e.charge().phase(), e.animState(), e.position()));
            }
        });
        helper.runAfterDelay(checkAt, () -> check.accept(start[0], samples));
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private static int count(List<Sample> s, ChargeTimeline.Phase p) {
        return (int) s.stream().filter(x -> x.phase() == p).count();
    }

    private static Sample first(List<Sample> s, ChargeTimeline.Phase p) {
        return s.stream().filter(x -> x.phase() == p).findFirst().orElse(null);
    }

    private static void assertStatesSynced(GameTestHelper helper, List<Sample> samples) {
        for (Sample s : samples) {
            helper.assertTrue(s.state() == MythAnimState.of(s.phase()), "同步状态 " + s.state() + " 与阶段 " + s.phase() + " 不一致");
        }
    }

    /** TC-T7-43：蓄力 20 刻 → 冲刺 → 收势 16 刻 → 空闲；只在冲刺时位移；同步状态一致。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH, timeoutTicks = 120)
    public static void charge_full_cycle(GameTestHelper helper) {
        floor(helper);
        MinotaurEntity e = spawn(helper, 5.5, 1.5);
        chargeScenario(helper, e, 8, 5, 65, (start, samples) -> {
            helper.assertTrue(count(samples, ChargeTimeline.Phase.WINDUP) == 19 || count(samples, ChargeTimeline.Phase.WINDUP) == 20,
                    "蓄力应为 20 刻（采样 " + count(samples, ChargeTimeline.Phase.WINDUP) + "）");
            // 与蓄力一样允许 1 刻采样抖动（2026-10-16：整套并行时 11 轮里 2 次采到 15）
            int recover16 = count(samples, ChargeTimeline.Phase.RECOVER);
            helper.assertTrue(recover16 == 15 || recover16 == 16, "收势应为 16 刻（采样 " + recover16 + "）");
            helper.assertTrue(count(samples, ChargeTimeline.Phase.RUN) == 10, "8 格冲刺应为 10 刻（采样 " + count(samples, ChargeTimeline.Phase.RUN) + "）");
            Sample run = first(samples, ChargeTimeline.Phase.RUN);
            Sample recover = first(samples, ChargeTimeline.Phase.RECOVER);
            Sample none = first(samples, ChargeTimeline.Phase.NONE);
            helper.assertTrue(run != null && recover != null && none != null, "冲锋没有走完");
            helper.assertTrue(horizontal(run.pos(), start) < 0.01, "蓄力期间不应位移");
            helper.assertTrue(horizontal(none.pos(), recover.pos()) < 0.01, "收势期间不应位移");
            double moved = none.pos().z - start.z;
            helper.assertTrue(Math.abs(moved - 8) < 0.5 && Math.abs(none.pos().x - start.x) < 0.05,
                    "应沿 +Z 直线冲刺约 8 格，实际 Δz=" + moved);
            assertStatesSynced(helper, samples);
            helper.assertTrue(e.animState() == MythAnimState.AUTO && !e.charge().active(), "结束后应回到空闲");
            helper.succeed();
        });
    }

    /** TC-T7-44：冲刺途中撞墙提前收势，不穿墙，不再继续位移。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH, timeoutTicks = 120)
    public static void charge_stops_at_wall(GameTestHelper helper) {
        floor(helper);
        for (int x = 0; x < 11; x++) {
            for (int y = 1; y <= 4; y++) {
                helper.setBlock(x, y, 6, Blocks.STONE);
            }
        }
        MinotaurEntity e = spawn(helper, 5.5, 1.5);
        BlockPos wall = helper.absolutePos(new BlockPos(5, 1, 6));
        chargeScenario(helper, e, 16, 5, 65, (start, samples) -> {
            int run = count(samples, ChargeTimeline.Phase.RUN);
            helper.assertTrue(run > 0 && run < 20, "撞墙应提前结束冲刺（冲刺 " + run + " 刻）");
            Sample recover = first(samples, ChargeTimeline.Phase.RECOVER);
            Sample none = first(samples, ChargeTimeline.Phase.NONE);
            helper.assertTrue(recover != null && none != null, "撞墙后应收势并回到空闲");
            helper.assertTrue(horizontal(none.pos(), recover.pos()) < 0.01, "收势后不应继续位移");
            helper.assertTrue(e.getBoundingBox().maxZ <= wall.getZ() + EPS, "不应穿进墙内：maxZ=" + e.getBoundingBox().maxZ + " 墙=" + wall.getZ());
            assertStatesSynced(helper, samples);
            helper.succeed();
        });
    }

    /** TC-T7-45：存档往返保留变体；进行中的冲锋不恢复。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void save_load_keeps_variant_drops_charge(GameTestHelper helper) {
        floor(helper);
        MinotaurEntity e = spawn(helper, 5.5, 1.5);
        e.setVariant("boss");
        e.startCharge(8);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(e.charge().active(), "冲锋应仍在进行");
            CompoundTag tag = new CompoundTag();
            e.saveWithoutId(tag);
            MinotaurEntity copy = ModEntities.MINOTAUR.get().create(helper.getLevel());
            helper.assertTrue(copy != null, "无法创建实体");
            copy.load(tag);
            helper.assertTrue("boss".equals(copy.variant()) && Math.abs(copy.variantScale() - 1.4F) < EPS, "变体未保留");
            helper.assertTrue(Math.abs(copy.getBbHeight() - 3.5) < EPS, "读档后尺寸应为 Boss 尺寸");
            helper.assertTrue(!copy.charge().active() && copy.animState() == MythAnimState.AUTO, "读档后不应继续冲锋");
            helper.succeed();
        });
    }
}
