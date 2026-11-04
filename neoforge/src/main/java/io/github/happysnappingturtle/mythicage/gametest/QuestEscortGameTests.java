package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.conduct.ConductService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.ResidentMigration;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * T21 阶段 ③：护送在途移民（单元测试用例文档 TC-T21-75～80）。测试文明 {@code questtest}；移民不开 AI、手动登记到达，
 * 在场采样直接调 {@link QuestService#sample}（指定此刻，每 20 刻一次），整段在同一刻里跑完，调度器插不进来。
 * 移民在 (8, 2, 24)，床位 (8, 2, 30)；陪着的玩家站在 (8, 2, 20)，走开的站到 40 格外。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestEscortGameTests {
    private static final String BATCH = "quest_escort";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private QuestEscortGameTests() {
    }

    static VillageContext ctx(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
    }

    /** 一名在途移民（同 {@code ResidentMigration.spawn}：登记为本村的在途移民，再发护送任务）。 */
    static ResidentEntity migrant(GameTestHelper h, VillageWorkGameTests.TV tv) {
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 24));
        BlockPos bed = h.absolutePos(new BlockPos(8, 2, 30));
        ResidentEntity r = Residents.spawnMigrant(h.getLevel(), tv.village(), VillageData.civ(tv.village().civ()).orElseThrow(),
                QuestGameTests.WORKER, bed, at).orElseThrow();
        r.setNoAi(true);
        VillageContext c = ctx(h, tv);
        r.setMigrant(true, c.now());
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        s.migrant = r.getUUID();
        s.migrantSince = c.now();
        QuestService.onMigrantSpawned(c, r);
        return r;
    }

    /** 走到床位登记（{@code ResidentMigration.Arrive} 的结算）。 */
    static void arrive(GameTestHelper h, VillageWorkGameTests.TV tv, ResidentEntity r) {
        JobType.REGISTRY.get(ResidentMigration.ARRIVE).settle(ctx(h, tv), r, null);
    }

    static Quest escort(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return QuestGameTests.only(h, tv, "escort:");
    }

    /** 从此刻起每 20 刻采样一次，共 {@code n} 次；返回最后一次的刻。 */
    static long samples(GameTestHelper h, VillageWorkGameTests.TV tv, long from, int n) {
        long t = from;
        for (int i = 0; i < n; i++) {
            t += 20;
            QuestService.sample(ctx(h, tv), t);
        }
        return t;
    }

    static void away(GameTestHelper h, FakePlayer p) {
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 20))).add(40, 0, 0));
    }

    static void near(GameTestHelper h, FakePlayer p) {
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 20))));
    }

    /** TC-T21-75：移民出发发护送任务（个人任务）；A 接后 B 被拒；A 放弃后 B 能接。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_published_personal(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e75a", 8, 20);
        FakePlayer b = QuestGameTests.player(h, "e75b", 9, 20);
        Quest q = escort(h, tv);
        h.assertTrue(q.scope == Quest.Scope.PERSONAL && q.target.equals(m.getUUID().toString()), "个人任务，目标是移民");
        h.assertTrue(!q.label.isEmpty(), "记下移民的名字");
        h.assertTrue(QuestGameTests.act(a, tv, q, QuestService.ACCEPT).ok(), "A 接");
        h.assertTrue(QuestGameTests.act(b, tv, q, QuestService.ACCEPT).result().equals("taken"), "B：已有人护送");
        h.assertTrue(QuestGameTests.act(a, tv, q, QuestService.ABANDON).result().equals("abandoned"), "A 放弃");
        h.assertTrue(QuestGameTests.act(b, tv, q, QuestService.ACCEPT).ok(), "B 能接了");
        QuestGameTests.cleanup(h, tv, a, b);
        h.succeed();
    }

    /** TC-T21-76：A 一路在移民 24 格内，移民到达：绿宝石定额 3、贡献 5；一路平安不加善。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_success_quiet(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e76", 8, 20);
        Quest q = escort(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        int before = ConductService.score(a);
        samples(h, tv, ctx(h, tv).now(), 10);
        arrive(h, tv, m);
        h.assertTrue(q.state == QuestState.SETTLED, "完成并结算：" + q.state + "/" + q.reason);
        h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 3, "绿宝石 3：" + QuestGameTests.count(a, Items.EMERALD));
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 5, "贡献 5");
        h.assertTrue(ConductService.score(a) == before, "一路平安不加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-77：A 接了以后走开（在场 30%）：移民到达后失效「你没有一路陪着」，没有报酬。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_absent(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e77", 8, 20);
        Quest q = escort(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        long t = samples(h, tv, ctx(h, tv).now(), 3);
        away(h, a);
        samples(h, tv, t, 7);
        arrive(h, tv, m);
        h.assertTrue(q.reason.equals("absent") && (q.state == QuestState.INVALID || q.state == QuestState.SETTLED),
                "在场不足：" + q.state + "/" + q.reason);
        h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 0, "没有报酬");
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 0, "没有贡献");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-78：途中尸壳盯上移民，A 打死它，移民到达：成功并加善 +2。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_real_danger(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e78", 8, 20);
        Quest q = escort(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        int before = ConductService.score(a);
        Husk z = SoldierGameTests.husk(h, 8.5, 27.5, true);
        z.setTarget(m);
        h.assertTrue(ThreatWatch.watchForTest(m.getUUID()).map(w -> w.threats.containsKey(z.getUUID())).orElse(false), "记下了威胁");
        long t = samples(h, tv, ctx(h, tv).now(), 2);
        z.hurt(a.damageSources().playerAttack(a), 1000);
        samples(h, tv, t, 8);
        h.assertTrue(q.vars.getOrDefault("danger", 0L) == 1L, "真实危险：" + q.vars);
        arrive(h, tv, m);
        h.assertTrue(q.state == QuestState.SETTLED, "完成：" + q.state + "/" + q.reason);
        h.assertTrue(ConductService.score(a) == before + 2, "加善 +2：" + (ConductService.score(a) - before));
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-79：途中的尸壳是刷怪蛋刷的（玩家造成的危险）：成功只给报酬，不加善。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_player_made_danger(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e79", 8, 20);
        Quest q = escort(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        int before = ConductService.score(a);
        Husk z = SoldierGameTests.husk(h, 8.5, 27.5, true);
        ThreatWatch.markPlayerMade(z);
        z.setTarget(m);
        long t = samples(h, tv, ctx(h, tv).now(), 2);
        z.hurt(a.damageSources().playerAttack(a), 1000);
        samples(h, tv, t, 8);
        arrive(h, tv, m);
        h.assertTrue(q.state == QuestState.SETTLED, "完成：" + q.state + "/" + q.reason);
        h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 3, "报酬照给");
        h.assertTrue(ConductService.score(a) == before, "不加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-80：移民途中被打死：护送失败（合同 §7「目标死亡」）；移民被放弃（24000 刻没到）：失败。 */
    @GameTest(template = T, batch = BATCH)
    public static void escort_target_dies(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = migrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "e80", 8, 20);
        Quest q = escort(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        m.hurt(h.getLevel().damageSources().generic(), 1000);
        h.assertTrue(q.state == QuestState.FAILED && q.reason.equals("dead"), "移民死了：" + q.state + "/" + q.reason);
        h.assertTrue(QuestGameTests.count(a, Items.EMERALD) == 0, "不结算");
        // 第二名移民没走到、被放弃
        VillageWorkGameTests.state(h, tv).migrant = null;
        ResidentEntity m2 = migrant(h, tv);
        Quest q2 = QuestGameTests.quests(h, tv).stream().filter(x -> x.target.equals(m2.getUUID().toString())).findFirst().orElseThrow();
        VillageWorkGameTests.state(h, tv).migrant = null;
        m2.discard();
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q2.state == QuestState.FAILED && q2.reason.equals("lost"), "被放弃：" + q2.state + "/" + q2.reason);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    // ───────────── D10：在途移民寻不到路时 ─────────────

    /**
     * 床位 (8, 2, 30) 用 7 × 7 的实心石头围死（走不进去；墙外离床位 4 格，超过「到了」的 2 格容差）；移民开着 AI 从 (8, 2, 14) 出发。寻路只能走到墙外，再寻就寻不到路。
     * 用开发命令那样登记在途移民，再让调度器派「走到床位」。
     */
    static ResidentEntity walledMigrant(GameTestHelper h, VillageWorkGameTests.TV tv) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(5, 1, 27), new BlockPos(11, 4, 33))) {
            h.setBlock(p, net.minecraft.world.level.block.Blocks.STONE);
        }
        h.setBlock(new BlockPos(8, 2, 30), net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(new BlockPos(8, 3, 30), net.minecraft.world.level.block.Blocks.AIR);
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 14));
        BlockPos bed = h.absolutePos(new BlockPos(8, 2, 30));
        ResidentEntity r = Residents.spawnMigrant(h.getLevel(), tv.village(), VillageData.civ(tv.village().civ()).orElseThrow(),
                QuestGameTests.WORKER, bed, at).orElseThrow();
        VillageContext c = ctx(h, tv);
        r.setMigrant(true, c.now());
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        s.migrant = r.getUUID();
        s.migrantSince = c.now();
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals(ResidentMigration.ARRIVE)).orElse(false), "派了「走到床位」");
        return r;
    }

    /** D10：附近没有玩家时，寻不到路的在途移民照旧回到床位（传送兜底不变）。 */
    // 超时 600 → 800（GA-1）：走路改成 2 米/秒后，走到墙边 12 格约 120 刻，再加「卡住」兜底 400 刻，600 刻太紧
    @GameTest(template = T, batch = "quest_escort_walk_a", timeoutTicks = 800)
    public static void migrant_unreachable_alone_teleports(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = walledMigrant(h, tv);
        BlockPos bed = h.absolutePos(new BlockPos(8, 2, 30));
        h.succeedWhen(() -> {
            h.assertTrue(VillageWorkGameTests.state(h, tv).roster.containsKey(m.getUUID()), "回到床位登记：" + m.blockPosition()
                    .subtract(h.absolutePos(BlockPos.ZERO)).toShortString());
            h.assertTrue(m.blockPosition().equals(bed), "在床位上");
            QuestGameTests.cleanup(h, tv);
        });
    }

    /** D10：16 格内有玩家（可能在护送）时，寻不到路也不传送到床位。 */
    @GameTest(template = T, batch = "quest_escort_walk_b", timeoutTicks = 600)
    public static void migrant_unreachable_watched_stays(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity m = walledMigrant(h, tv);
        FakePlayer a = QuestGameTests.player(h, "d10", 8, 24);
        h.runAtTickTime(560, () -> {  // 超过「卡住」兜底的 400 刻
            h.assertTrue(!VillageWorkGameTests.state(h, tv).roster.containsKey(m.getUUID()) && m.inTransit(), "没有传送进床位");
            h.assertTrue(m.blockPosition().getZ() - h.absolutePos(BlockPos.ZERO).getZ() >= 20, "走到了墙外：" + m.blockPosition()
                    .subtract(h.absolutePos(BlockPos.ZERO)).toShortString());
            QuestGameTests.cleanup(h, tv, a);
            h.succeed();
        });
    }
}
