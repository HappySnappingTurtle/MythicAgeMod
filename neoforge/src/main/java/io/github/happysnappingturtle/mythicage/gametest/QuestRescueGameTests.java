package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.conduct.ConductService;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.content.greek.power.SentinelEntity;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.ThreatWatch;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.Husk;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * T21 阶段 ③：敌袭救援（单元测试用例文档 TC-T21-81～89）。测试文明 {@code questtest}（士兵那条用 T20 的测试文明）；
 * 受威胁的居民在 (8, 2, 12)、不开 AI；尸壳不开 AI，用 {@code setTarget} 盯上居民（同目标选择的事件）。
 * 警戒用 {@code raiseAlarm} 开一场、{@code alarmForTest} 拉长或结束；判定直接调 {@link QuestService#sample}。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestRescueGameTests {
    private static final String BATCH = "quest_rescue";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private QuestRescueGameTests() {
    }

    static ResidentEntity ward(GameTestHelper h, VillageWorkGameTests.TV tv) {
        ResidentEntity r = QuestGameTests.spawn(h, tv, QuestGameTests.WORKER, 8, 12, false);
        r.setNoAi(true);
        return r;
    }

    /** 开一场警戒（拉长到足够久）；返回场次。 */
    static long alarm(GameTestHelper h, VillageWorkGameTests.TV tv) {
        long now = h.getLevel().getGameTime();
        long serial = VillageScheduler.raiseAlarm(tv.id(), now);
        VillageScheduler.alarmForTest(tv.id(), now + 100000);
        return serial;
    }

    static void calm(GameTestHelper h, VillageWorkGameTests.TV tv) {
        VillageScheduler.alarmForTest(tv.id(), h.getLevel().getGameTime() - 1);
        step(h, tv);
    }

    static void step(GameTestHelper h, VillageWorkGameTests.TV tv) {
        QuestService.sample(QuestEscortGameTests.ctx(h, tv), h.getLevel().getGameTime());
    }

    static Husk threat(GameTestHelper h, ResidentEntity ward, double z) {
        Husk m = SoldierGameTests.husk(h, 8.5, z, true);
        m.setTarget(ward);
        return m;
    }

    static Quest rescue(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return QuestGameTests.only(h, tv, "rescue:");
    }

    static void hit(Husk m, FakePlayer p, float dmg) {
        m.invulnerableTime = 0;
        m.hurt(p.damageSources().playerAttack(p), dmg);
    }

    /** TC-T21-81：警戒中尸壳盯上居民：发救援事件任务，不占名额（已接 3 个也照样参与）。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_published_event(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x81", 8, 16);
        long serial = alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        Quest q = rescue(h, tv);
        h.assertTrue(q.scope == Quest.Scope.EVENT && q.source.equals("rescue:" + serial + ":" + w.getUUID()), "事件任务：" + q.source);
        h.assertTrue(QuestRules.activeFor(QuestGameTests.quests(h, tv), a.getUUID()) == 0, "不占名额");
        h.assertTrue(QuestGameTests.act(a, tv, q, QuestService.ACCEPT).result().equals("event"), "不用接");
        step(h, tv);
        h.assertTrue(QuestGameTests.quests(h, tv).stream().filter(x -> x.source.startsWith("rescue:")).count() == 1, "同一场不重复发");
        m.discard();
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-82：A 打死尸壳，居民活到警戒结束：善 +3、贡献 3，收据 rescue:<村>:<场次>:<居民>。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_success(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x82", 8, 16);
        int before = ConductService.score(a);
        long serial = alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        Quest q = rescue(h, tv);
        h.assertTrue(q.state == QuestState.OPEN && q.contrib.containsKey(a.getUUID()), "记下 A，等警戒结束：" + q.state);
        calm(h, tv);
        h.assertTrue(q.state == QuestState.SETTLED, "完成并结算：" + q.state + "/" + q.reason);
        h.assertTrue(ConductService.score(a) == before + 3, "善 +3：" + (ConductService.score(a) - before));
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 3, "贡献 3");
        h.assertTrue(MythData.get(a).receipts().contains(ReceiptBook.Scope.UNIQUE, "rescue:" + tv.id() + ":" + serial + ":" + w.getUUID()),
                "善恶收据");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-83：A 40%、B 35%、士兵打出致死一击：没人救下，任务失效，无人加善。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_soldier_kill(GameTestHelper h) {
        VillageWorkGameTests.TV tv = SoldierGameTests.village(h);
        List<ResidentEntity> ws = SoldierGameTests.workers(h, tv, 3);
        ResidentEntity w = ws.get(2);
        w.setNoAi(true);
        ResidentEntity soldier = SoldierGameTests.enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        FakePlayer a = QuestGameTests.player(h, "x83a", 8, 16);
        FakePlayer b = QuestGameTests.player(h, "x83b", 9, 16);
        int sa = ConductService.score(a);
        int sb = ConductService.score(b);
        alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        float max = m.getMaxHealth();
        hit(m, a, max * 0.4F);
        hit(m, b, max * 0.35F);
        m.invulnerableTime = 0;
        m.hurt(h.getLevel().damageSources().mobAttack(soldier), 1000);
        step(h, tv);
        calm(h, tv);
        Quest q = rescue(h, tv);
        h.assertTrue(q.reason.equals("no_rescuer") && q.contrib.isEmpty(), "没人救下：" + q.state + "/" + q.reason + " " + q.contrib);
        h.assertTrue(ConductService.score(a) == sa && ConductService.score(b) == sb, "无人加善");
        io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(a, false);
        io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(b, false);
        SoldierGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-84：居民被打死：失败。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_ward_dies(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x84", 8, 16);
        int before = ConductService.score(a);
        alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        w.hurt(h.getLevel().damageSources().generic(), 1000);
        Quest q = rescue(h, tv);
        h.assertTrue(q.state == QuestState.FAILED && q.reason.equals("dead"), "失败：" + q.state + "/" + q.reason);
        calm(h, tv);
        h.assertTrue(ConductService.score(a) == before, "不加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-85：A 先打尸壳（挑衅），尸壳再去盯居民，A 打死它：对 A 不算真实危险。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_provoked(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x85", 8, 16);
        int before = ConductService.score(a);
        alarm(h, tv);
        Husk m = SoldierGameTests.husk(h, 8.5, 15.5, true);
        hit(m, a, 1);
        m.setTarget(w);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        calm(h, tv);
        Quest q = rescue(h, tv);
        h.assertTrue(q.reason.equals("no_rescuer") && !q.contrib.containsKey(a.getUUID()), "不算救下：" + q.state + "/" + q.reason);
        h.assertTrue(ConductService.score(a) == before, "不加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-86：A 自己伤过这名居民，再打死盯上他的尸壳：不加善。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_harmed_ward(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x86", 8, 16);
        w.hurt(a.damageSources().playerAttack(a), 1);
        int after = ConductService.score(a);
        alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        calm(h, tv);
        Quest q = rescue(h, tv);
        h.assertTrue(!q.contrib.containsKey(a.getUUID()), "不算救下：" + q.contrib);
        h.assertTrue(ConductService.score(a) == after, "伤人之后没有再加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-87：同一场警戒，居民先后被两只尸壳盯上，A 都打死：善只加一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_once_per_alert(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x87", 8, 16);
        int before = ConductService.score(a);
        alarm(h, tv);
        Husk m1 = threat(h, w, 15.5);
        step(h, tv);
        hit(m1, a, 1000);
        step(h, tv);
        Husk m2 = threat(h, w, 9.5);
        step(h, tv);
        hit(m2, a, 1000);
        step(h, tv);
        h.assertTrue(QuestGameTests.quests(h, tv).stream().filter(x -> x.source.startsWith("rescue:")).count() == 1, "一条任务");
        calm(h, tv);
        h.assertTrue(ConductService.score(a) == before + 3, "只加一次 +3：" + (ConductService.score(a) - before));
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 3, "贡献只记一次");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-88：善恶日上限：A 当天已 +11，再救一次只加 1（ConductService 的正向上限，任务不另算）。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_daily_cap(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x88", 8, 16);
        MythData.get(a).conduct().addPositive(QuestGameTests.today(h), 11);
        int before = ConductService.score(a);
        alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        calm(h, tv);
        h.assertTrue(ConductService.score(a) == before + 1, "只加 1：" + (ConductService.score(a) - before));
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-89：A 的召唤物（哨兵石像）打死尸壳：算 A 救下（Responsibility 的召唤归属）。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_by_summon(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x89", 8, 16);
        int before = ConductService.score(a);
        SentinelEntity s = GreekPowers.SENTINEL_ENTITY.get().create(h.getLevel());
        s.setup(a.getUUID(), h.getLevel().getGameTime() + 10000);
        alarm(h, tv);
        Husk m = threat(h, w, 15.5);
        step(h, tv);
        m.hurt(h.getLevel().damageSources().mobAttack(s), 1000);
        step(h, tv);
        calm(h, tv);
        h.assertTrue(ConductService.score(a) == before + 3, "召唤物打死算 A：" + (ConductService.score(a) - before));
        s.discard();
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** 玩家刷出来的尸壳（刷怪蛋、命令、刷怪笼标记）：救了也不加善（TC-T21-37 的实机版）。 */
    @GameTest(template = T, batch = BATCH)
    public static void rescue_player_made(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        ResidentEntity w = ward(h, tv);
        FakePlayer a = QuestGameTests.player(h, "x37", 8, 16);
        int before = ConductService.score(a);
        alarm(h, tv);
        Husk m = SoldierGameTests.husk(h, 8.5, 15.5, true);
        ThreatWatch.markPlayerMade(m);
        m.setTarget(w);
        step(h, tv);
        hit(m, a, 1000);
        step(h, tv);
        calm(h, tv);
        h.assertTrue(ConductService.score(a) == before, "不加善");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }
}
