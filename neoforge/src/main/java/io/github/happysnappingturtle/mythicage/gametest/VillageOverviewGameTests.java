package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.conduct.ConductService;
import io.github.happysnappingturtle.mythicage.conduct.ConductView;
import io.github.happysnappingturtle.mythicage.conduct.ConductViewService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.rite.RiteService;
import io.github.happysnappingturtle.mythicage.village.VillageBounds;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T24 阶段 ①：善恶与贡献、村庄总览、村界、阶段提升记录（单元测试用例文档 TC-T24-30～45）。
 * 沿用 T21 的测试文明 {@code questtest}（铜锭本村没有来源、石砖有配方），场地与谷仓管事位置同 {@link QuestGameTests}。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageOverviewGameTests {
    private static final String BATCH = "t24_village_ui";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private VillageOverviewGameTests() {
    }

    // ───────────── 善恶与贡献 ─────────────

    /** TC-T24-30：视图带本人分值、档名、当日正分、最近事件（新的在前）。 */
    @GameTest(template = T, batch = BATCH)
    public static void conduct_view_own(GameTestHelper h) {
        FakePlayer a = QuestGameTests.player(h, "t24c30", 8, 12);
        ConductService.change(a, "cure_zombie_villager", 3);
        ConductService.change(a, "harm_villager", -2);
        ConductView v = ConductViewService.build(a);
        h.assertTrue(v.enabled() && v.score() == 1 && v.tier().equals("ordinary"), "分值 1、平常：" + v.score() + " " + v.tier());
        h.assertTrue(v.recent().size() == 2 && v.recent().get(0).reason().equals("harm_villager") && v.recent().get(0).delta() == -2,
                "新的在前：" + v.recent());
        h.assertTrue(v.positiveToday() == 3 && v.positiveCap() > 0, "今日正分 3：" + v.positiveToday());
        QuestGameTests.cleanupPlayers(a);
        h.succeed();
    }

    /** TC-T24-31：两个玩家各自的视图互不混入（请求不带玩家参数，服务端只认发送者）。 */
    @GameTest(template = T, batch = BATCH)
    public static void conduct_view_private(GameTestHelper h) {
        FakePlayer a = QuestGameTests.player(h, "t24c31a", 8, 12);
        FakePlayer b = QuestGameTests.player(h, "t24c31b", 9, 12);
        ConductService.change(a, "harm_villager", -5);
        ConductView vb = ConductViewService.build(b);
        h.assertTrue(vb.score() == 0 && vb.recent().isEmpty(), "B 看不到 A 的事件：" + vb.recent());
        QuestGameTests.cleanupPlayers(a, b);
        h.succeed();
    }

    /** TC-T24-32：交付委托结算后，累计贡献与来源记录一起写；视图右栏列出该村。 */
    @GameTest(template = T, batch = BATCH)
    public static void contribution_logged_on_settle(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.project(h, tv, QuestGameTests.COPPER, 4, QuestGameTests.today(h));
        FakePlayer a = QuestGameTests.player(h, "t24c32", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 4));
        QuestGameTests.scan(h, tv);
        Quest q = QuestGameTests.only(h, tv, "supply:project:");
        h.assertTrue(QuestGameTests.act(a, tv, q, QuestService.DELIVER).ok(), "交付");
        VillageQuests.Book b = VillageQuests.get(h.getLevel().getServer()).peek(tv.id()).orElseThrow();
        List<ContributionLog.Event> log = b.contributionLog.recent(a.getUUID(), 8);
        h.assertTrue(log.size() == 1 && log.get(0).reason().equals("supply") && log.get(0).points() == 4, "来源记录：" + log);
        ConductView v = ConductViewService.build(a);
        h.assertTrue(v.villages().size() == 1 && v.villages().get(0).points() == 4, "右栏一个村、4 分：" + v.villages());
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-33：村务捐献也记贡献（与交付同权重）。 */
    @GameTest(template = T, batch = BATCH)
    public static void contribution_on_donate(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        FakePlayer a = QuestGameTests.player(h, "t24c33", 8, 12);
        int got = VillageAffairs.donateForTest(a, tv.id(), new ItemStack(Items.COPPER_INGOT, 3));
        int points = QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID());
        h.assertTrue(got == 3 && points == 3, "捐 3 件贡献 3：" + got + " / " + points);
        var log = VillageQuests.get(h.getLevel().getServer()).peek(tv.id()).orElseThrow().contributionLog.recent(a.getUUID(), 8);
        h.assertTrue(log.size() == 1 && log.get(0).reason().equals("donate"), "来源是捐献：" + log);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-34：两个村都有贡献：右栏按贡献从高到低。 */
    @GameTest(template = T, batch = BATCH)
    public static void conduct_villages_sorted(GameTestHelper h) {
        VillageWorkGameTests.TV one = QuestGameTests.village(h);
        VillageWorkGameTests.TV two = QuestGameTests.village(h);
        FakePlayer a = QuestGameTests.player(h, "t24c34", 8, 12);
        long today = QuestGameTests.today(h);
        VillageQuests.get(h.getLevel().getServer()).book(one.id()).contribute(a.getUUID(), "supply", 2, today);
        VillageQuests.get(h.getLevel().getServer()).book(two.id()).contribute(a.getUUID(), "escort", 10, today);
        ConductView v = ConductViewService.build(a);
        h.assertTrue(v.villages().size() == 2 && v.villages().get(0).points() == 10 && v.villages().get(1).points() == 2,
                "高的在前：" + v.villages());
        h.assertTrue(v.villages().get(0).recent().get(0).reason().equals("escort"), "最近来源");
        QuestGameTests.cleanup(h, two);
        QuestGameTests.cleanup(h, one, a);
        h.succeed();
    }

    // ───────────── 村庄总览 ─────────────

    /** TC-T24-36：总览的居民数、岗位计数与库存要点。 */
    @GameTest(template = T, batch = BATCH)
    public static void overview_people(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.spawn(h, tv, QuestGameTests.WORKER, 8, 10, true);
        QuestGameTests.spawn(h, tv, QuestGameTests.WORKER, 6, 10, false);
        QuestGameTests.spawn(h, tv, QuestGameTests.SMITH, 10, 10, false);
        FakePlayer a = QuestGameTests.player(h, "t24v36", 8, 12);
        AffairsView v = VillageAffairs.view(a, tv.id()).orElseThrow();
        AffairsView.Overview o = v.overview();
        h.assertTrue(v.tab() == AffairsView.TAB_OVERVIEW, "默认总览页");
        h.assertTrue(o.residents() == 3, "居民 3：" + o.residents());
        h.assertTrue(o.jobs().size() == 2 && o.jobs().get(0).n() == 2, "两种岗位、工人 2 个排前：" + o.jobs());
        h.assertTrue(!o.food().key().isEmpty(), "有口粮行");
        h.assertTrue(o.project().isEmpty() && o.quests() == 0, "没有在建、没有委托");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-37：同一项目缺「有配方的石砖」与「本村没有来源的铜锭」：拆成自主与稀有两条。 */
    @GameTest(template = T, batch = BATCH)
    public static void overview_blockers_split(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        BuildProject p = QuestGameTests.project(h, tv, QuestGameTests.COPPER, 4, QuestGameTests.today(h));
        p.planned.put(QuestGameTests.BRICKS, 6.0);
        FakePlayer a = QuestGameTests.player(h, "t24v37", 8, 12);
        List<AffairsView.Blocker> bs = VillageAffairs.view(a, tv.id()).orElseThrow().blockers();
        long rare = bs.stream().filter(b -> b.cls().equals("rare_material")).count();
        long self = bs.stream().filter(b -> b.cls().equals("self_resolving")
                && b.line().key().equals("mythic_age.village.short.material")).count();
        h.assertTrue(rare == 1 && self == 1, "稀有 1、自主 1：" + bs);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-38：停工被占：受阻里有一条「被占地」，带坐标。 */
    @GameTest(template = T, batch = BATCH)
    public static void overview_blocked_plot(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        BuildProject p = QuestGameTests.project(h, tv, QuestGameTests.COPPER, 1, QuestGameTests.today(h));
        p.block(new Pos(1, 2, 3), "player_block");
        FakePlayer a = QuestGameTests.player(h, "t24v38", 8, 12);
        List<AffairsView.Blocker> bs = VillageAffairs.view(a, tv.id()).orElseThrow().blockers();
        h.assertTrue(bs.stream().anyMatch(b -> b.cls().equals("occupied") && b.line().args().contains("1, 2, 3")), "被占地：" + bs);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-39：协作者：贡献 &gt; 0 的前 8 名，降序；0 分不列；查不到名字的不列。 */
    @GameTest(template = T, batch = BATCH)
    public static void overview_collaborators(GameTestHelper h) {
        Map<UUID, Integer> contribution = new java.util.LinkedHashMap<>();
        Map<UUID, String> names = new java.util.HashMap<>();
        for (int i = 0; i < 10; i++) {
            UUID id = UUID.randomUUID();
            names.put(id, "t24h" + i);
            contribution.put(id, i);
        }
        contribution.put(UUID.randomUUID(), 99);
        List<AffairsView.Count> c = io.github.happysnappingturtle.mythicage.village.economy.VillageOverview.collaborators(contribution,
                id -> java.util.Optional.ofNullable(names.get(id)));
        h.assertTrue(c.size() == 8, "前 8 名：" + c);
        h.assertTrue(c.get(0).key().equals("t24h9") && c.get(0).n() == 9 && c.get(0).literal(), "最高的在前：" + c.get(0));
        h.assertTrue(c.stream().noneMatch(x -> x.n() <= 0), "0 分不列");
        h.succeed();
    }

    /** TC-T24-40：村界：已建并集的边、未建规划地块、被占地块。 */
    @GameTest(template = T, batch = BATCH)
    public static void bounds_three_kinds(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        var server = h.getLevel().getServer();
        VillagePlans plans = VillagePlans.get(server);
        VillageInfoPiece.Info i = plans.byVillage(tv.id()).orElseThrow().info();
        int ox = h.absolutePos(net.minecraft.core.BlockPos.ZERO).getX();
        int oz = h.absolutePos(net.minecraft.core.BlockPos.ZERO).getZ();
        List<VillagePlan.Plot> plots = List.of(
                new VillagePlan.Plot("p1", "house", "s0", new Box(ox + 1, 2, oz + 1, ox + 3, 4, oz + 3), "south"),
                new VillagePlan.Plot("p2", "house", "s1", new Box(ox + 5, 2, oz + 1, ox + 7, 4, oz + 3), "south"),
                new VillagePlan.Plot("p3", "house", "s1", new Box(ox + 9, 2, oz + 1, ox + 11, 4, oz + 3), "south"));
        plans.updateInfo(tv.id(), new VillageInfoPiece.Info(i.civ(), i.patron(), i.layout(), i.layoutVersion(), i.templateSet(),
                i.center(), i.anchors(), i.built(), plots));
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        s.builtPlots.add("p1");
        BuildProject p = new BuildProject(s.nextProjectId++, "questtest_hut", "p3", "", "", new Pos(0, 0, 0), Turn.R0, null, null,
                QuestGameTests.today(h));
        p.block(new Pos(ox + 10, 2, oz + 2), "player_block");
        s.projects.add(p);
        AffairsPayloads.Bounds b = VillageBounds.build(server, tv.id()).orElseThrow();
        h.assertTrue(!b.built().isEmpty(), "有已建村界");
        h.assertTrue(b.planned().size() == 1 && b.planned().get(0)[0] == ox + 5, "未建规划只有 p2：" + b.planned().size());
        h.assertTrue(b.occupied().size() == 1 && b.occupied().get(0)[0] == ox + 9, "被占的是 p3");
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-T24-42：村庄阶段提升：记录写「升为某阶段」与每项变化；文字里没有恩宠。 */
    @GameTest(template = T, batch = BATCH)
    public static void stage_up_logged(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        FakePlayer a = QuestGameTests.player(h, "t24v42", 8, 12);
        var before = tv.village().withStage(Age.ARCHAIC);
        var after = tv.village().withStage(Age.CLASSICAL);
        RiteService.announceStage(a, before, after);
        List<VillageEconomy.LogEntry> log = VillageWorkGameTests.state(h, tv).log;
        h.assertTrue(log.stream().anyMatch(e -> e.key().equals("mythic_age.village.log.stage_up")), "记录了阶段提升");
        h.assertTrue(log.stream().noneMatch(e -> e.key().contains("favor") || e.key().contains("grace")), "不提恩宠");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-43：三种载荷的编解码往返。 */
    @GameTest(template = T, batch = BATCH)
    public static void payload_roundtrip(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.project(h, tv, QuestGameTests.COPPER, 2, QuestGameTests.today(h));
        FakePlayer a = QuestGameTests.player(h, "t24v43", 8, 12);
        ConductService.change(a, "harm_villager", -1);
        ConductView cv = ConductViewService.build(a);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ConductView.STREAM_CODEC.encode(buf, cv);
        h.assertTrue(ConductView.STREAM_CODEC.decode(buf).equals(cv), "善恶视图往返");
        AffairsView av = VillageAffairs.view(a, tv.id(), AffairsView.TAB_STOCK).orElseThrow();
        buf = new FriendlyByteBuf(Unpooled.buffer());
        AffairsView.STREAM_CODEC.encode(buf, av);
        AffairsView back = AffairsView.STREAM_CODEC.decode(buf);
        h.assertTrue(back.equals(av) && back.tab() == AffairsView.TAB_STOCK, "村务视图往返");
        AffairsPayloads.Bounds bounds = VillageBounds.build(h.getLevel().getServer(), tv.id()).orElseThrow();
        buf = new FriendlyByteBuf(Unpooled.buffer());
        AffairsPayloads.Bounds.STREAM_CODEC.encode(buf, bounds);
        AffairsPayloads.Bounds bb = AffairsPayloads.Bounds.STREAM_CODEC.decode(buf);
        h.assertTrue(bb.built().equals(bounds.built()) && bb.planned().size() == bounds.planned().size(), "村界往返");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T24-44：任务簿存档往返带贡献来源；旧档（没有该字段）读作空、累计贡献保留。 */
    @GameTest(template = T, batch = BATCH)
    public static void contribution_log_save(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        var server = h.getLevel().getServer();
        UUID me = UUID.randomUUID();
        VillageQuests q = VillageQuests.get(server);
        q.book(tv.id()).contribute(me, "repair", 5, 3);
        VillageQuests back = VillageQuests.roundTripForTest(q, server.registryAccess());
        var log = back.peek(tv.id()).orElseThrow().contributionLog.recent(me, 8);
        h.assertTrue(log.size() == 1 && log.get(0).equals(new ContributionLog.Event("repair", 5, 3)), "往返：" + log);
        CompoundTag tag = q.save(new CompoundTag(), server.registryAccess());
        for (net.minecraft.nbt.Tag t : tag.getList("villages", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            ((CompoundTag) t).remove("contributionLog");
        }
        VillageQuests old = VillageQuests.read(tag, server.registryAccess());
        VillageQuests.Book ob = old.peek(tv.id()).orElseThrow();
        h.assertTrue(ob.contributionLog.recent(me, 8).isEmpty() && ob.contribution(me) == 5, "旧档：记录空、累计保留");
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-T24-45：看村务的资格：谷仓管事身边可以；离得远、又没有祭司会话不行；「从祭司打开」没有会话时不打开。 */
    @GameTest(template = T, batch = BATCH)
    public static void affairs_reach(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.spawn(h, tv, QuestGameTests.WORKER, 8, 10, true);
        FakePlayer near = QuestGameTests.player(h, "t24v45n", 8, 11);
        FakePlayer far = QuestGameTests.player(h, "t24v45f", 8, 30);
        h.runAfterDelay(5, () -> {
            h.assertTrue(VillageAffairs.resolve(near, tv.id()).isPresent(), "管事身边可以");
            h.assertTrue(VillageAffairs.resolve(far, tv.id()).isEmpty(), "远处不行");
            h.assertTrue(VillageAffairs.resolve(near, AffairsPayloads.Open.FROM_PRIEST).isEmpty(), "没有祭司会话");
            QuestGameTests.cleanup(h, tv, near, far);
            h.succeed();
        });
    }
}
