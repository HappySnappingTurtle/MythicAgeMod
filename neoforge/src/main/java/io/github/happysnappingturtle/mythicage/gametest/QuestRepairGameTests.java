package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.RepairJob;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * T21 阶段 ②：修复设施（单元测试用例文档 TC-T21-60～71）。测试村的建筑盒覆盖整个场地；在 (4–7, 2–3, 20) 砌一面石砖墙当设施。
 * 玩家打方块走假玩家的真实破坏流程（触发 {@code BreakEvent}）；放方块先放再发 {@code EntityPlaceEvent}（同玩家放置的顺序）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestRepairGameTests {
    private static final String BATCH = "quest_repair";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private QuestRepairGameTests() {
    }

    static List<BlockPos> wall(GameTestHelper h) {
        return wall(h, Blocks.STONE_BRICKS);
    }

    static List<BlockPos> wall(GameTestHelper h, net.minecraft.world.level.block.Block block) {
        List<BlockPos> out = new java.util.ArrayList<>();
        for (int x = 4; x <= 7; x++) {
            BlockPos p = new BlockPos(x, 2, 20);
            h.setBlock(p, block);
            out.add(p);
        }
        return out;
    }

    static void breakBy(GameTestHelper h, FakePlayer p, BlockPos rel) {
        p.gameMode.destroyBlock(h.absolutePos(rel));
    }

    static void place(GameTestHelper h, FakePlayer p, BlockPos rel, BlockState st) {
        BlockPos abs = h.absolutePos(rel);
        h.getLevel().setBlock(abs, st, 3);
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(h.getLevel().dimension(), h.getLevel(), abs),
                Blocks.AIR.defaultBlockState(), p));
    }

    static VillageQuests.Book book(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageQuests.get(h.getLevel().getServer()).book(tv.id());
    }

    static int cells(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return book(h, tv).damage.values().stream().mapToInt(d -> d.cells.size()).sum();
    }

    static VillageQuests.Cell cell(GameTestHelper h, VillageWorkGameTests.TV tv, BlockPos rel) {
        long k = h.absolutePos(rel).asLong();
        return book(h, tv).damage.values().stream().map(d -> d.cells.get(k)).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    static Quest repair(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return QuestGameTests.only(h, tv, "repair:");
    }

    /** TC-T21-60：玩家打掉 4 块：损坏记 4 格（原方块、责任人）；日开始发修复任务，需求 4。 */
    @GameTest(template = T, batch = BATCH)
    public static void damage_recorded_and_published(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r60", 8, 18);
        wall.forEach(p -> breakBy(h, a, p));
        h.assertTrue(cells(h, tv) == 4, "记 4 格：" + cells(h, tv));
        VillageQuests.Cell c = cell(h, tv, wall.get(0));
        h.assertTrue(c != null && c.block.equals("minecraft:stone_bricks") && a.getUUID().equals(c.cause), "原方块与责任人");
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        h.assertTrue(q.need == 4 && q.prices.get("repair") == 1.0, "需求 4：" + q.need);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-61：苦力怕在墙边爆炸：损坏格无主。 */
    @GameTest(template = T, batch = BATCH)
    public static void explosion_unowned(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        wall(h);
        Creeper c = EntityType.CREEPER.create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(5, 2, 21));
        c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.getLevel().addFreshEntity(c);
        h.getLevel().explode(c, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.MOB);
        c.discard();
        h.assertTrue(cells(h, tv) > 0, "炸掉的记了：" + cells(h, tv));
        h.assertTrue(book(h, tv).damage.values().stream().flatMap(d -> d.cells.values().stream()).allMatch(x -> x.cause == null), "无主");
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-62：作物、农田不算设施。 */
    @GameTest(template = T, batch = BATCH)
    public static void crops_not_facility(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        h.setBlock(new BlockPos(10, 1, 10), Blocks.FARMLAND);
        h.setBlock(new BlockPos(10, 2, 10), Blocks.WHEAT);
        FakePlayer a = QuestGameTests.player(h, "r62", 8, 12);
        breakBy(h, a, new BlockPos(10, 2, 10));
        breakBy(h, a, new BlockPos(10, 1, 10));
        h.assertTrue(cells(h, tv) == 0, "不记：" + cells(h, tv));
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-63：玩家自己放进去的方块被打掉：不算损坏。 */
    @GameTest(template = T, batch = BATCH)
    public static void player_block_not_damage(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        FakePlayer a = QuestGameTests.player(h, "r63", 8, 12);
        BlockPos p = new BlockPos(10, 2, 12);
        place(h, a, p, Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(book(h, tv).playerBlocks.contains(h.absolutePos(p).asLong()), "记成玩家方块");
        breakBy(h, a, p);
        h.assertTrue(cells(h, tv) == 0 && !book(h, tv).playerBlocks.contains(h.absolutePos(p).asLong()), "不记损坏");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-64：村庄自己改方块（施工、拆旧直接改世界）：不记损坏。 */
    @GameTest(template = T, batch = BATCH)
    public static void village_edits_not_damage(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        wall.forEach(p -> h.getLevel().setBlock(h.absolutePos(p), Blocks.AIR.defaultBlockState(), 3));
        h.assertTrue(cells(h, tv) == 0, "不记");
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-T21-65：B（不是打掉的人）接任务、放回 4 块：完成，B 按 4 格结算。 */
    @GameTest(template = T, batch = BATCH)
    public static void repair_by_other(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r65a", 8, 18);
        FakePlayer b = QuestGameTests.player(h, "r65b", 9, 18);
        wall.forEach(p -> breakBy(h, a, p));
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        h.assertTrue(QuestGameTests.act(b, tv, q, QuestService.ACCEPT).ok(), "B 接取");
        wall.forEach(p -> place(h, b, p, Blocks.STONE_BRICKS.defaultBlockState()));
        h.assertTrue(q.state == QuestState.SETTLED && QuestGameTests.count(b, Items.EMERALD) == 4, "B 4 格：" + q.state + " "
                + QuestGameTests.count(b, Items.EMERALD));
        h.assertTrue(QuestService.contribution(h.getLevel().getServer(), tv.id(), b.getUUID()) == 4, "贡献 4");
        QuestGameTests.cleanup(h, tv, a, b);
        h.succeed();
    }

    /** TC-T21-66：A（打掉的人）自己修 4 块：都算修好，A 报酬与贡献 0。 */
    @GameTest(template = T, batch = BATCH)
    public static void repair_self_caused(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r66", 8, 18);
        wall.forEach(p -> breakBy(h, a, p));
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        QuestGameTests.act(a, tv, q, QuestService.ACCEPT);
        wall.forEach(p -> place(h, a, p, Blocks.STONE_BRICKS.defaultBlockState()));
        h.assertTrue(cells(h, tv) == 0 && q.done == 4, "4 格修好");
        h.assertTrue(q.state.terminal() && QuestGameTests.count(a, Items.EMERALD) == 0
                && QuestService.contribution(h.getLevel().getServer(), tv.id(), a.getUUID()) == 0, "不给报酬：" + q.state);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-67：没接任务的人放回原方块：格子算修好，不记到他名下；全被他修好时任务「已被他人补上」。 */
    @GameTest(template = T, batch = BATCH)
    public static void repair_without_accepting(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r67a", 8, 18);
        FakePlayer c = QuestGameTests.player(h, "r67c", 9, 18);
        wall.forEach(p -> breakBy(h, a, p));
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        wall.forEach(p -> place(h, c, p, Blocks.STONE_BRICKS.defaultBlockState()));
        h.assertTrue(q.state.terminal() && q.reason.equals("satisfied") && QuestGameTests.count(c, Items.EMERALD) == 0,
                "已被他人补上：" + q.state + " " + q.reason);
        QuestGameTests.cleanup(h, tv, a, c);
        h.succeed();
    }

    /** TC-T21-68：同一格同一日反复拆修：第二次不记损坏。 */
    @GameTest(template = T, batch = BATCH)
    public static void same_day_no_rerecord(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r68a", 8, 18);
        FakePlayer b = QuestGameTests.player(h, "r68b", 9, 18);
        breakBy(h, a, wall.get(0));
        place(h, b, wall.get(0), Blocks.STONE_BRICKS.defaultBlockState());
        breakBy(h, a, wall.get(0));
        h.assertTrue(cells(h, tv) == 0, "第二次不记：" + cells(h, tv));
        QuestGameTests.cleanup(h, tv, a, b);
        h.succeed();
    }

    /** TC-T21-69：到期没人修：转村民自修，有料就放回原方块；没料发「为修缮备料」供给任务（墙用村里产不了的磨制安山岩）。 */
    @GameTest(template = T, batch = BATCH)
    public static void expired_self_repair(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h, Blocks.POLISHED_ANDESITE);
        FakePlayer a = QuestGameTests.player(h, "r69", 8, 18);
        wall.forEach(p -> breakBy(h, a, p));
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        q.deadline = QuestGameTests.today(h);
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state.terminal() && q.reason.equals("expired"), "过期：" + q.state);
        h.assertTrue(book(h, tv).damage.values().stream().flatMap(d -> d.cells.values().stream()).allMatch(c -> c.self), "转自修");
        QuestGameTests.scan(h, tv);
        h.assertTrue(QuestGameTests.quests(h, tv).stream().anyMatch(x -> x.source.equals("supply:repair:minecraft:polished_andesite")),
                "没料：发为修缮备料（磨制安山岩本村产不了，当天就发）");
        VillageWorkGameTests.state(h, tv).stock.putUnchecked("minecraft:polished_andesite", 4);
        RepairJob job = new RepairJob();
        var ctx = VillageScheduler.contextOf(h.getLevel(), tv.id()).orElseThrow();
        List<Job> jobs = job.candidates(ctx);
        h.assertTrue(!jobs.isEmpty() && jobs.get(0).inputs().get("minecraft:polished_andesite") == 1, "有料就派自修单：" + jobs.size());
        ResidentEntity r = QuestGameTests.spawn(h, tv, QuestGameTests.WORKER, 6, 18, false);
        for (Job j : jobs) {
            h.assertTrue(job.settle(ctx, r, j), "放回");
        }
        h.assertTrue(wall.stream().allMatch(p -> h.getBlockState(p).is(Blocks.POLISHED_ANDESITE)) && cells(h, tv) == 0, "墙补好了");
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }

    /** TC-T21-71：建筑整座不在了（规划里没有这个建筑盒）：修复任务「场地失效」。 */
    @GameTest(template = T, batch = BATCH)
    public static void site_gone(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        List<BlockPos> wall = wall(h);
        FakePlayer a = QuestGameTests.player(h, "r71", 8, 18);
        wall.forEach(p -> breakBy(h, a, p));
        QuestGameTests.scan(h, tv);
        Quest q = repair(h, tv);
        VillagePlans plans = VillagePlans.get(h.getLevel().getServer());
        VillageInfoPiece.Info i = tv.plan().info();
        plans.updateInfo(tv.id(), new VillageInfoPiece.Info(i.civ(), i.patron(), i.layout(), i.layoutVersion(), i.templateSet(),
                i.center(), i.anchors(), List.of(), i.plots()));
        QuestService.refreshNow(h.getLevel().getServer(), tv.id());
        h.assertTrue(q.state.terminal() && q.reason.equals("site"), "场地失效：" + q.state + " " + q.reason);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }
}
