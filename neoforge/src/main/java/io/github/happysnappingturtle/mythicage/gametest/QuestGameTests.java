package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentDialogue;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.QuestView;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * T21 阶段 ①：任务框架与缺料供给（单元测试用例文档 TC-T21-40～57）。用测试文明 {@code questtest}（铜锭本村没有来源、
 * 石砖有配方、面包是口粮）证明框架不依赖希腊；口粮任务用另一个文明 id {@code questfood}（口粮底线不同，T20 经验：变体用不同 id）。
 * 场地 17 × 16 × 36，谷仓管事在 (8, 2, 10)，玩家默认站在管事旁边。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestGameTests {
    private static final String BATCH = "quest_supply";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final String CIV = "questtest";
    static final String FOOD_CIV = "questfood";
    static final String WORKER = "questtest:worker";
    static final String SMITH = "questtest:smith";
    static final String COPPER = "minecraft:copper_ingot";
    static final String BRICKS = "minecraft:stone_bricks";

    private QuestGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static CivVillageData civ(String id, int foodFloor) {
        CivVillageData d = CivVillageData.parse(id, JsonParser.parseString("""
                {"schedule": {"segments": [{"id": "work", "from": 0, "to": 24000}]},
                 "storage": {"base": 256, "unit": 0},
                 "rations": {"floor": %d, "portions": {"minecraft:bread": 1.0, "minecraft:baked_potato": 0.5}},
                 "residents": {"fallback": "%s", "keeper": "%s", "male": ["Tester"], "female": ["Testa"]},
                 "buildCosts": {"groups": {"planks": ["minecraft:oak_planks", "minecraft:spruce_planks"]}},
                 "quests": {"supplyItems": {"minecraft:copper_ingot": 3}, "prices": {"default": 1.0}, "repairPrice": 1.0}}"""
                .formatted(foodFloor, WORKER, WORKER)).getAsJsonObject());
        var brick = io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe.parse("questtest:brick", id,
                JsonParser.parseString("""
                        {"station": "minecraft:crafting_table", "professions": ["questtest:worker"],
                         "inputs": {"minecraft:cobblestone": 2}, "outputs": {"minecraft:stone_bricks": 1},
                         "stages": [{"anim": "work", "ticks": 20}]}""").getAsJsonObject());
        CivVillageData.Profession worker = new CivVillageData.Profession(WORKER, Set.of(), "archaic", false);
        CivVillageData.Profession smith = new CivVillageData.Profession(SMITH, Set.of(), "archaic", false, List.of(),
                new CivVillageData.ShortWhen("", List.of(COPPER), false), null);
        return d.withProfessions(Map.of(WORKER, worker, SMITH, smith), Map.of(brick.id(), brick));
    }

    static VillageWorkGameTests.TV village(GameTestHelper h, String civ, int foodFloor) {
        VillageData.overrideForTest(civ, civ(civ, foodFloor));
        VillageWorkGameTests.floor(h);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, civ, Map.of(),
                new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35));
        VillageWorkGameTests.takeOver(h, tv);
        return tv;
    }

    static VillageWorkGameTests.TV village(GameTestHelper h) {
        return village(h, CIV, 0);
    }

    static ResidentEntity spawn(GameTestHelper h, VillageWorkGameTests.TV tv, String prof, int x, int z, boolean keeper) {
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        return Residents.spawn(h.getLevel(), tv.village(), VillageData.civ(tv.village().civ()).orElseThrow(), prof, at, at, keeper)
                .orElseThrow();
    }

    static FakePlayer player(GameTestHelper h, String name, int x, int z) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x, 2, z))));
        p.getInventory().clearContent();
        OnlinePlayers.forTest(p, true);
        return p;
    }

    /** 一个账单固定的测试项目（建筑 id 不存在：施工的定时处理会跳过它，只剩任务在动它）。 */
    static BuildProject project(GameTestHelper h, VillageWorkGameTests.TV tv, String item, int n, long startDay) {
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        BuildProject p = new BuildProject(s.nextProjectId++, "questtest_hut", "", "", "", new Pos(0, 0, 0), Turn.R0, null, null,
                startDay);
        p.planned.put(item, (double) n);
        s.projects.add(p);
        return p;
    }

    static long today(GameTestHelper h) {
        return MythDays.today(h.getLevel().getServer());
    }

    static List<Quest> quests(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return QuestService.quests(h.getLevel().getServer(), tv.id());
    }

    static Quest only(GameTestHelper h, VillageWorkGameTests.TV tv, String sourcePrefix) {
        List<Quest> qs = quests(h, tv).stream().filter(q -> q.source.startsWith(sourcePrefix)).toList();
        h.assertTrue(qs.size() == 1, "来源 " + sourcePrefix + " 应有 1 条任务：" + qs.stream().map(q -> q.source + "/" + q.state).toList());
        return qs.get(0);
    }

    static int count(FakePlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    static QuestService.Outcome act(FakePlayer p, VillageWorkGameTests.TV tv, Quest q, String action) {
        return QuestService.act(p, tv.id(), q.id(), action, UUID.randomUUID().toString(), false);
    }

    static void scan(GameTestHelper h, VillageWorkGameTests.TV tv) {
        QuestService.scanNow(h.getLevel().getServer(), tv.id());
    }

    static void cleanupPlayers(FakePlayer... players) {
        for (FakePlayer p : players) {
            OnlinePlayers.forTest(p, false);
        }
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv, FakePlayer... players) {
        for (FakePlayer p : players) {
            OnlinePlayers.forTest(p, false);
        }
        VillageWorkGameTests.cleanup(h, tv);
    }

    // ───────────── 用例 ─────────────

    /** TC-T21-40：项目缺「本村无来源」的铜锭 6：日开始发 1 条供给任务，需求 6；再扫不重复发。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_published_once(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        scan(h, tv);
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        h.assertTrue(q.need == 6 && q.state == QuestState.OPEN && q.target.equals(COPPER), "需求 6：" + q.need + " " + q.state);
        h.assertTrue(VillageWorkGameTests.state(h, tv).log.stream().anyMatch(e -> e.key().equals("mythic_age.village.log.quest_posted")),
                "村务记录写了「发出委托」");
        cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-41：背包 10 个铜锭交付：只收 6，村库 +6，任务完成并结算；绿宝石 6、贡献 6、收据一张。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_deliver_settles(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        BuildProject p = project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q41", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 10));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        QuestService.Outcome o = act(a, tv, q, QuestService.DELIVER);
        h.assertTrue(o.ok() && o.delivered() == 6, "交了 6：" + o);
        h.assertTrue(count(a, Items.COPPER_INGOT) == 4, "背包剩 4：" + count(a, Items.COPPER_INGOT));
        h.assertTrue(p.missing().isEmpty(), "项目不再缺铜：" + p.missing());
        h.assertTrue(q.state == QuestState.SETTLED, "完成并结算：" + q.state);
        h.assertTrue(count(a, Items.EMERALD) == 6, "绿宝石 6：" + count(a, Items.EMERALD));
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 6, "贡献 6");
        h.assertTrue(MythData.get(a).receipts().contains(ReceiptBook.Scope.UNIQUE, QuestService.receiptId(tv.id(), q)), "收据");
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-42：同一 requestId 交两次（双击 / 重发）：第二次不动背包与村库。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_duplicate_request(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q42", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 3));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        QuestService.Outcome first = QuestService.act(a, tv.id(), q.id(), QuestService.DELIVER, "req-42", false);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 3));
        QuestService.Outcome second = QuestService.act(a, tv.id(), q.id(), QuestService.DELIVER, "req-42", false);
        h.assertTrue(first.delivered() == 3 && second.result().equals("duplicate"), "第二次重复：" + first + " / " + second);
        h.assertTrue(count(a, Items.COPPER_INGOT) == 3 && q.done == 3, "背包与进度不变：" + count(a, Items.COPPER_INGOT) + " " + q.done);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-43：A 交 4、B 交 2，共同完成：各按自己的量结算，各一张收据。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_shared(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q43a", 8, 12);
        FakePlayer b = player(h, "q43b", 9, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 4));
        b.getInventory().add(new ItemStack(Items.COPPER_INGOT, 5));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        act(a, tv, q, QuestService.DELIVER);
        act(b, tv, q, QuestService.DELIVER);
        h.assertTrue(q.state == QuestState.SETTLED, "完成：" + q.state);
        h.assertTrue(count(a, Items.EMERALD) == 4 && count(b, Items.EMERALD) == 2, "A 4、B 2：" + count(a, Items.EMERALD) + " / "
                + count(b, Items.EMERALD));
        h.assertTrue(count(b, Items.COPPER_INGOT) == 3, "B 只交了还差的 2");
        cleanup(h, tv, a, b);
        h.succeed();
    }

    /** TC-T21-44：A 交 2 后有人捐 4 补上缺口：任务「需求已满足」，A 按 2 件结算，捐赠者无报酬。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_covered_by_donation(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q44a", 8, 12);
        FakePlayer donor = player(h, "q44d", 9, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 2));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        act(a, tv, q, QuestService.DELIVER);
        VillageAffairs.donateForTest(donor, tv.id(), new ItemStack(Items.COPPER_INGOT, 4));
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state == QuestState.SETTLED && q.reason.equals("satisfied"), "需求已满足：" + q.state + " " + q.reason);
        h.assertTrue(count(a, Items.EMERALD) == 2 && count(donor, Items.EMERALD) == 0, "A 按 2 件结算");
        cleanup(h, tv, a, donor);
        h.succeed();
    }

    /** TC-T21-45：项目在任务进行中建成：任务失效；之后交付被拒，背包不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_project_done(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        BuildProject p = project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q45", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 5));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        p.state = BuildProject.State.DONE;
        QuestService.Outcome o = act(a, tv, q, QuestService.DELIVER);
        h.assertTrue(!o.ok() && count(a, Items.COPPER_INGOT) == 5, "被拒、背包不变：" + o);
        h.assertTrue(q.state == QuestState.SETTLED && q.reason.equals("gone"), "失效（没人交过直接结清）：" + q.state + " " + q.reason);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-46：截止日到：过期，已交部分结算。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_expired(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q46", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 2));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        h.assertTrue(q.deadline == today(h) + 3, "有效期 3 日：" + q.deadline);
        act(a, tv, q, QuestService.DELIVER);
        q.deadline = today(h);
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state == QuestState.SETTLED && q.reason.equals("expired"), "过期：" + q.state + " " + q.reason);
        h.assertTrue(count(a, Items.EMERALD) == 2, "已交 2 件结算");
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-47：已接 3 个任务，再接第 4 个被拒。 */
    @GameTest(template = T, batch = BATCH)
    public static void accept_limit(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        VillageQuests.Book b = VillageQuests.get(h.getLevel().getServer()).book(tv.id());
        for (int i = 0; i < 4; i++) {
            // 类型 manual：框架不认识的类型不会被刷新改状态，只测接取上限
            b.quests.add(new Quest(b.next++, "manual", "test:" + i, COPPER, Quest.Scope.PUBLIC, today(h), 9, today(h) + 3));
        }
        FakePlayer a = player(h, "q47", 8, 12);
        for (int i = 0; i < 3; i++) {
            h.assertTrue(act(a, tv, b.quests.get(i), QuestService.ACCEPT).ok(), "第 " + (i + 1) + " 个能接");
        }
        QuestService.Outcome o = act(a, tv, b.quests.get(3), QuestService.ACCEPT);
        h.assertTrue(o.result().equals("full"), "第 4 个拒绝：" + o);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-48：存档往返（重启）：进度、贡献、截止日不变；完成后只结算一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void quest_roundtrip(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q48", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 3));
        scan(h, tv);
        Quest before = only(h, tv, "supply:project:");
        act(a, tv, before, QuestService.DELIVER);
        VillageQuests all = VillageQuests.get(h.getLevel().getServer());
        all.replaceForTest(tv.id(), VillageQuests.roundTripForTest(all, h.getLevel().registryAccess()));
        Quest q = only(h, tv, "supply:project:");
        h.assertTrue(q != before && q.done == 3 && q.deadline == before.deadline && q.contrib.get(a.getUUID()) == 3
                && q.accepted.contains(a.getUUID()) && q.prices.get(COPPER) == 1.0, "读回一致");
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 3));
        act(a, tv, q, QuestService.DELIVER);
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state == QuestState.SETTLED && count(a, Items.EMERALD) == 6, "只结算一次：" + count(a, Items.EMERALD));
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-49：结算时 A 离线：报酬留着；A 上线进村时发放，收据只一张。 */
    @GameTest(template = T, batch = BATCH)
    public static void settle_offline_then_enter(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q49", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 2));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        act(a, tv, q, QuestService.DELIVER);
        OnlinePlayers.forTest(a, false);
        q.deadline = today(h);
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state == QuestState.EXPIRED && q.owes(a.getUUID()) && count(a, Items.EMERALD) == 0, "离线：待领 " + q.state);
        OnlinePlayers.forTest(a, true);
        QuestService.onEnter(a, tv.village());
        QuestService.onEnter(a, tv.village());
        h.assertTrue(q.state == QuestState.SETTLED && count(a, Items.EMERALD) == 2, "上线进村发放一次：" + count(a, Items.EMERALD));
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-50：结算中途崩溃（已有收据但没标已结算）：重启后只补标记，不重复发放。 */
    @GameTest(template = T, batch = BATCH)
    public static void settle_receipt_without_mark(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        FakePlayer a = player(h, "q50", 8, 12);
        VillageQuests.Book b = VillageQuests.get(h.getLevel().getServer()).book(tv.id());
        Quest q = new Quest(b.next++, "supply", "test:crash", COPPER, Quest.Scope.PUBLIC, today(h), 4, today(h) + 3);
        q.credit(a.getUUID(), 4, 4);
        q.move(QuestState.COMPLETE, today(h), "");
        b.quests.add(q);
        MythData.transact(a, Transactions.Request.unique(QuestService.receiptId(tv.id(), q)));
        QuestService.onEnter(a, tv.village());
        h.assertTrue(q.state == QuestState.SETTLED && count(a, Items.EMERALD) == 0, "只补标记：" + q.state + " " + count(a, Items.EMERALD));
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-51：配置关掉（热更）：未结束的暂停、不发新任务；打开后继续，截止日顺延。单独一批（改的是全局配置）。 */
    @GameTest(template = T, batch = "quest_config")
    public static void disable_pauses(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        VillagesConfig.Quests d = VillagesConfig.Quests.DEFAULTS;
        try {
            QuestService.overrideConfigForTest(new VillagesConfig.Quests(false, d.maxActivePerPlayer(), d.maxOpenPerVillage(),
                    d.supplyDays(), d.repairDays(), d.stockingGraceDays(), d.repairMinCells(), d.repairDailyCap(), d.escortRadius(),
                    d.escortPresence(), d.rescueRadius()));
            project(h, tv, BRICKS, 2, today(h) - 5);
            QuestService.refreshNow(h.getLevel().getServer(), tv.id());
            h.assertTrue(q.state == QuestState.PAUSED, "暂停：" + q.state);
            scan(h, tv);
            h.assertTrue(quests(h, tv).size() == 1, "关着不发新任务");
            QuestService.overrideConfigForTest(null);
            QuestService.refreshNow(h.getLevel().getServer(), tv.id());
            h.assertTrue(q.state == QuestState.OPEN && q.deadline == today(h) + 3, "恢复：" + q.state + " 截止 " + q.deadline);
        } finally {
            QuestService.overrideConfigForTest(null);
        }
        cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-52：距离核验：离管事 10 格被拒；只在普通居民身边也被拒（顺带验证捐赠的 nearKeeper 修正）。 */
    @GameTest(template = T, batch = BATCH)
    public static void reach_keeper_only(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        spawn(h, tv, WORKER, 8, 10, true);
        spawn(h, tv, WORKER, 8, 30, false);
        project(h, tv, COPPER, 6, today(h));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        FakePlayer far = player(h, "q52f", 8, 20);
        FakePlayer byWorker = player(h, "q52w", 8, 31);
        FakePlayer near = player(h, "q52n", 8, 11);
        h.runAfterDelay(5, () -> {
            h.assertTrue(QuestService.act(far, tv.id(), q.id(), QuestService.ACCEPT, "r52f", true).result().equals("far"), "10 格外被拒");
            h.assertTrue(QuestService.act(byWorker, tv.id(), q.id(), QuestService.ACCEPT, "r52w", true).result().equals("far"),
                    "普通居民身边被拒");
            h.assertTrue(QuestService.act(near, tv.id(), q.id(), QuestService.ACCEPT, "r52n", true).ok(), "管事身边可以");
            byWorker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_INGOT, 1));
            h.assertTrue(VillageAffairs.donate(byWorker, tv.id()) == 0, "捐赠也只认管事");
            cleanup(h, tv, far, byWorker, near);
            h.succeed();
        });
    }

    /** TC-T21-53：口粮跌破底线：发口粮任务；交面包与烤土豆按口粮值折算。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_food(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, FOOD_CIV, 10);
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        s.stock.take("minecraft:bread", 9999, 0);
        s.stock.putUnchecked("minecraft:bread", 6);
        scan(h, tv);
        Quest q = only(h, tv, "supply:food");
        h.assertTrue(q.need == 4, "还差 4 份：" + q.need);
        FakePlayer a = player(h, "q53", 8, 12);
        a.getInventory().add(new ItemStack(Items.BAKED_POTATO, 3));
        a.getInventory().add(new ItemStack(Items.BREAD, 10));
        QuestService.Outcome o = act(a, tv, q, QuestService.DELIVER);
        h.assertTrue(o.ok(), "交付：" + o);
        h.assertTrue(q.state == QuestState.SETTLED, "口粮回到底线：" + q.state + " 份 " + q.done);
        int bread = 10 - count(a, Items.BREAD);
        int potato = 3 - count(a, Items.BAKED_POTATO);
        h.assertTrue(bread + potato * 0.5 >= 4 && bread + (potato - 1) * 0.5 < 4 + 1, "按口粮值折算：面包 " + bread + " 土豆 " + potato);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-54：职业缺料（铜锭为 0、在 supplyItems 里）：发任务，需求 3（文明定量）。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_short(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        spawn(h, tv, SMITH, 4, 10, false);
        scan(h, tv);
        Quest q = only(h, tv, "supply:short:");
        h.assertTrue(q.need == 3 && q.target.equals(COPPER), "需求 3：" + q.need);
        FakePlayer a = player(h, "q54", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 5));
        act(a, tv, q, QuestService.DELIVER);
        h.assertTrue(q.state == QuestState.SETTLED && count(a, Items.COPPER_INGOT) == 2, "交 3 完成：" + q.state);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-55：本村能产的石砖：备料不到 1 日不发；超过 1 日没进展才发。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_producible_grace(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        BuildProject fresh = project(h, tv, BRICKS, 4, today(h));
        scan(h, tv);
        h.assertTrue(quests(h, tv).stream().noneMatch(q -> q.target.equals(BRICKS)), "不到 1 日不发");
        fresh.state = BuildProject.State.CANCELLED;
        project(h, tv, BRICKS, 4, today(h) - 2);
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        h.assertTrue(q.target.equals(BRICKS), "超过 1 日没进展：发");
        cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-20：物品组 #planks 还差 12：背包橡木板 4、云杉木板 10，两种都收，共 12。 */
    @GameTest(template = T, batch = BATCH)
    public static void supply_item_group(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, "#planks", 12, today(h) - 5);
        FakePlayer a = player(h, "q20", 8, 12);
        a.getInventory().add(new ItemStack(Items.OAK_PLANKS, 4));
        a.getInventory().add(new ItemStack(Items.SPRUCE_PLANKS, 10));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        QuestService.Outcome o = act(a, tv, q, QuestService.DELIVER);
        h.assertTrue(o.delivered() == 12 && count(a, Items.OAK_PLANKS) + count(a, Items.SPRUCE_PLANKS) == 2, "交了 12：" + o);
        h.assertTrue(q.state == QuestState.SETTLED, "完成：" + q.state);
        cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-56：居民对话：本职缺料且有对应任务时是「委托」。 */
    @GameTest(template = T, batch = BATCH)
    public static void dialogue_points_to_quest(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        ResidentEntity smith = spawn(h, tv, SMITH, 4, 10, false);
        h.assertTrue(ResidentDialogue.kind(h.getLevel().getServer(), smith) == ResidentDialogue.Kind.SHORT, "没任务时是本职缺料");
        scan(h, tv);
        h.assertTrue(ResidentDialogue.kind(h.getLevel().getServer(), smith) == ResidentDialogue.Kind.QUEST, "有任务时是委托");
        cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-57：委托页视图字段齐全；接满后其余行显示「已满」。 */
    @GameTest(template = T, batch = BATCH)
    public static void quest_view_fields(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        project(h, tv, COPPER, 6, today(h));
        FakePlayer a = player(h, "q57", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 2));
        scan(h, tv);
        Quest q = only(h, tv, "supply:project:");
        act(a, tv, q, QuestService.DELIVER);
        QuestView v = QuestService.view(a, tv.id()).orElseThrow();
        QuestView.Row r = v.rows().stream().filter(x -> x.id().equals(q.id())).findFirst().orElseThrow();
        h.assertTrue(r.title().key().equals("mythic_age.quest.title.supply.project") && r.done() == 2 && r.need() == 6
                && r.daysLeft() == 3 && r.scope().equals("public") && r.rewards().containsAll(List.of("emerald", "contribution"))
                && r.state().equals("open") && r.accepted() && r.canDeliver() && r.mine() == 2, "行：" + r);
        h.assertTrue(v.active() == 1 && v.max() == 3 && v.enabled(), "页脚：" + v.active() + "/" + v.max());
        cleanup(h, tv, a);
        h.succeed();
    }
}
