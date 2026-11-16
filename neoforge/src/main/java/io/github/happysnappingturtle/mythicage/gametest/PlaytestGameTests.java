package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.core.guardian.GuardianRules;
import io.github.happysnappingturtle.mythicage.handbook.Handbook;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.playtest.PlaytestJobs;
import io.github.happysnappingturtle.mythicage.playtest.PlaytestLog;
import io.github.happysnappingturtle.mythicage.playtest.PlaytestSetup;
import io.github.happysnappingturtle.mythicage.playtest.PlaytestState;
import io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * GA-3 快速验证的准备动作（TC-GA3-50～66）。GameTest 里没有真实生成的村庄和迷宫：
 * 找结构、传送、等载入这一段由实机脚本覆盖，这里直接测清场、按规格准备、神庙搭建与记录文件。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlaytestGameTests {
    private static final String BATCH = "playtest";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final Civilization GREEK = Civilization.GREEK;
    /** 神庙蓝图：祭坛在 (8, 3, 2)，占地 x 1–15、z 1–18、到 y 12。 */
    private static final BlockPos ALTAR = new BlockPos(8, 3, 2);

    private PlaytestGameTests() {
    }

    private static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 3, 1))));
        p.getInventory().clearContent();
        return p;
    }

    private static PlaytestStations.Spec spec(String id) {
        return PlaytestStations.byId(id).orElseThrow().spec();
    }

    private static int count(FakePlayer p, String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return p.getInventory().countItem(item);
    }

    private static void ground(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 2, z, Blocks.STONE);
            }
        }
    }

    private static void cleanup(GameTestHelper h, FakePlayer p) {
        PlaytestSetup.removeShrinesForTest(p);
        PlaytestState.get(h.getLevel().getServer()).takeSnapshotForTest(p.getUUID());
    }

    // ───────────── 清场与快照 ─────────────

    /** TC-GA3-50：清场后神话数据归零、名下没有祭坛、生存模式满血；手札回执保留。 */
    @GameTest(template = T, batch = BATCH)
    public static void reset_clears(GameTestHelper h) {
        FakePlayer p = player(h, "pt50");
        PlayerMythData.CivState s = MythData.get(p).civ(GREEK);
        s.setAge(Age.CLASSICAL);
        s.setFavor(80);
        MythData.get(p).receipts().record(ReceiptBook.Scope.UNIQUE, Handbook.RECEIPT);
        GlobalPos fake = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(3, 3, 30)));
        ShrineRegistry.get(h.getLevel().getServer()).add(new ShrineRegistry.Entry(fake, p.getUUID(), "pt50",
                net.minecraft.core.Direction.SOUTH, GREEK, GreekTemple.ALTAR_KIND.id()));
        p.setHealth(5);
        p.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        p.setExperienceLevels(5);
        long version = MythData.get(p).dataVersion();
        PlaytestSetup.reset(p, PlaytestState.get(h.getLevel().getServer()));
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0 && p.experienceLevel == 0, "背包与经验清空");
        h.assertTrue(MythData.get(p).dataVersion() > version, "版本号要比清场前大，客户端才收同步");
        PlayerMythData.CivState after = MythData.get(p).civ(GREEK);
        h.assertTrue(after.age() == Age.NONE && after.favor() == 0, "时代、恩宠归零：" + after.age() + " " + after.favor());
        h.assertTrue(ShrineRegistry.get(h.getLevel().getServer()).get(fake).isEmpty(), "名下祭坛已删");
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "满血");
        h.assertTrue(p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL, "生存模式");
        h.assertTrue(MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, Handbook.RECEIPT), "手札回执保留");
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-51：恢复后与第一次进站前相同；第二次进站不覆盖快照。 */
    @GameTest(template = T, batch = BATCH)
    public static void snapshot_restore(GameTestHelper h) {
        FakePlayer p = player(h, "pt51");
        PlaytestState st = PlaytestState.get(h.getLevel().getServer());
        MythData.get(p).civ(GREEK).setAge(Age.CLASSICAL);
        MythData.get(p).civ(GREEK).setFavor(80);
        MythData.get(p).firstKills().add("mythic_age:minotaur");
        p.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        p.setExperienceLevels(7);
        h.assertTrue(PlaytestSetup.reset(p, st), "第一次进站");
        PlaytestSetup.apply(p, spec("powers"));
        h.assertFalse(PlaytestSetup.reset(p, st), "第二次不再存快照");
        long version = MythData.get(p).dataVersion();
        h.assertTrue(PlaytestSetup.restore(p, st), "能恢复");
        h.assertTrue(MythData.get(p).dataVersion() > version, "恢复后版本号也往上走");
        PlayerMythData d = MythData.get(p);
        h.assertTrue(d.civ(GREEK).age() == Age.CLASSICAL && d.civ(GREEK).favor() == 80
                && d.firstKills().contains("mythic_age:minotaur"), "恢复成第一次进站前：" + d.civ(GREEK).age() + " " + d.civ(GREEK).favor());
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 5 && p.experienceLevel == 7,
                "背包与经验也恢复：钻石 " + p.getInventory().countItem(Items.DIAMOND) + " 级 " + p.experienceLevel);
        h.assertTrue(count(p, "mythic_age:greek_altar") == 0, "上一站给的东西不留");
        h.assertFalse(st.hasSnapshot(p.getUUID()), "快照用掉");
        h.assertFalse(PlaytestSetup.restore(p, st), "没有快照时失败");
        cleanup(h, p);
        h.succeed();
    }

    // ───────────── 各站的准备 ─────────────

    /** TC-GA3-52：起步站：未受祝福，有手札。 */
    @GameTest(template = T, batch = BATCH)
    public static void station_start(GameTestHelper h) {
        FakePlayer p = player(h, "pt52");
        PlaytestSetup.reset(p, PlaytestState.get(h.getLevel().getServer()));
        List<String> missing = PlaytestSetup.apply(p, spec("start"));
        h.assertTrue(missing.isEmpty(), "物品都找到：" + missing);
        h.assertTrue(MythData.get(p).civ(GREEK).age() == Age.NONE, "未受祝福");
        h.assertTrue(p.getInventory().contains(Handbook.stack()), "有手札");
        h.assertTrue(MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, Handbook.RECEIPT), "手札回执");
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-53：神庙站：远古、祝福回执与事件、祭坛与一级材料。 */
    @GameTest(template = T, batch = BATCH)
    public static void station_temple(GameTestHelper h) {
        FakePlayer p = player(h, "pt53");
        List<String> events = new ArrayList<>();
        BiConsumer<net.minecraft.server.level.ServerPlayer, MythEvents.Fired> l = (sp, f) -> {
            if (f.player().equals(p.getUUID())) {
                events.add(f.event());
            }
        };
        MythEvents.addListener(l);
        try {
            PlaytestSetup.reset(p, PlaytestState.get(h.getLevel().getServer()));
            List<String> missing = PlaytestSetup.apply(p, spec("temple"));
            h.assertTrue(missing.isEmpty(), "物品都找到：" + missing);
        } finally {
            MythEvents.removeListener(l);
        }
        h.assertTrue(MythData.get(p).civ(GREEK).age() == Age.ARCHAIC, "远古");
        h.assertTrue(MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, GuardianRules.blessingReceipt("greek")), "祝福回执");
        h.assertTrue(events.contains(MythEvents.BLESSED) && events.contains(MythEvents.AGE_REACHED), "事件：" + events);
        // GA-13：受祝福不再发祭坛（temple 站在 GA-13 阶段 ⑤ 改成「神庙扩建」站）
        h.assertTrue(count(p, "mythic_age:greek_altar") == 0, "不发祭坛");
        // GA-13：神庙扩建站不再发建神庙的材料（材料放进村库，见 VillageTempleGameTests.playtest_upgrade_starts）
        h.assertTrue(count(p, "mythic_age:marble_pillar") == 0, "不发神庙材料");
        h.assertTrue(count(p, "mythic_age:libation") == 1, "奠酒 1（试供奉）");
        h.assertTrue(p.getInventory().contains(Handbook.stack()), "每站都有手札");
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-54：共用的祝福方法连调两次只成功一次（GA-13：不发祭坛）。 */
    @GameTest(template = T, batch = BATCH)
    public static void blessing_once(GameTestHelper h) {
        FakePlayer p = player(h, "pt54");
        h.assertTrue(TradeService.grantBlessing(p, GREEK, true), "第一次成功");
        h.assertFalse(TradeService.grantBlessing(p, GREEK, true), "第二次拒绝");
        h.assertTrue(count(p, "mythic_age:greek_altar") == 0, "不发祭坛：" + count(p, "mythic_age:greek_altar"));
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-55：神力站：古典 + 雅典娜，恩宠 150。 */
    @GameTest(template = T, batch = BATCH)
    public static void station_powers(GameTestHelper h) {
        FakePlayer p = player(h, "pt55");
        PlaytestSetup.reset(p, PlaytestState.get(h.getLevel().getServer()));
        PlaytestSetup.apply(p, spec("powers"));
        PlayerMythData.CivState s = MythData.get(p).civ(GREEK);
        h.assertTrue(s.age() == Age.CLASSICAL, "古典：" + s.age());
        h.assertTrue("athena".equals(s.minorGods().get(Age.CLASSICAL)), "次神雅典娜：" + s.minorGods());
        h.assertTrue(s.favor() == 150, "恩宠 150：" + s.favor());
        h.assertTrue(count(p, "mythic_age:greek_altar") == 0, "预先搭神庙的站不发祭坛");
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-56：村庄有缺料工程时，ensureQuest 之后至少一件开放的委托。 */
    @GameTest(template = T, batch = "quest_supply")
    public static void station_village_quest(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.project(h, tv, "minecraft:copper_ingot", 6, QuestGameTests.today(h));
        int open = PlaytestSetup.ensureQuest(h.getLevel(), tv.id());
        h.assertTrue(open >= 1, "开放的委托：" + open);
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** A3：村庄站把贡献补到点名门槛 64：从 10 补到 64；再进一次不重复加；本来就有 100 的不动。 */
    @GameTest(template = T, batch = "quest_supply")
    public static void station_village_contribution(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        java.util.UUID low = java.util.UUID.randomUUID();
        java.util.UUID rich = java.util.UUID.randomUUID();
        var book = io.github.happysnappingturtle.mythicage.village.quest.VillageQuests.get(h.getLevel().getServer()).book(tv.id());
        book.contribute(low, "test", 10, 0);
        book.contribute(rich, "test", 100, 0);
        int target = io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations.byId("village").orElseThrow().spec().contribution();
        h.assertTrue(PlaytestSetup.ensureContribution(h.getLevel(), tv.id(), low, target) == 64, "10 补到 64：" + book.contribution(low));
        h.assertTrue(PlaytestSetup.ensureContribution(h.getLevel(), tv.id(), low, target) == 64, "再进一次不重复加");
        h.assertTrue(PlaytestSetup.ensureContribution(h.getLevel(), tv.id(), rich, target) == 100, "本来够的不动");
        QuestGameTests.cleanup(h, tv);
        h.succeed();
    }

    /** TC-GA3-57、58：仪式站给一半古典供品与 50 恩宠；Boss 站有线团、复原神力、青铜全套。 */
    @GameTest(template = T, batch = BATCH)
    public static void station_rite_and_boss(GameTestHelper h) {
        FakePlayer p = player(h, "pt57");
        PlaytestState st = PlaytestState.get(h.getLevel().getServer());
        PlaytestSetup.reset(p, st);
        PlaytestSetup.apply(p, spec("rite"));
        h.assertTrue(MythData.get(p).civ(GREEK).favor() == 50, "恩宠 50");
        h.assertTrue(p.getInventory().countItem(Items.COOKED_BEEF) == 8 && p.getInventory().countItem(Items.WHEAT) == 32,
                "熟牛肉 8、小麦 32");
        h.assertTrue(count(p, "mythic_age:libation") == 0 && p.getInventory().countItem(Items.COPPER_INGOT) == 0, "奠酒、铜锭缺");
        p.getInventory().clearContent();
        PlaytestSetup.reset(p, st);
        List<String> missing = PlaytestSetup.apply(p, spec("boss"));
        h.assertTrue(missing.isEmpty(), "物品都找到：" + missing);
        for (String id : List.of("mythic_age:ariadne_thread", "mythic_age:gp_gr_restoration", "mythic_age:bronze_helmet",
                "mythic_age:bronze_chestplate", "mythic_age:bronze_leggings", "mythic_age:bronze_boots", "mythic_age:xiphos",
                "mythic_age:aspis")) {
            h.assertTrue(count(p, id) == 1, id);
        }
        cleanup(h, p);
        h.succeed();
    }

    /** TC-GA3-59：英雄站：首杀、凭证、图纸、图鉴；英雄供品齐；恩宠 100。 */
    @GameTest(template = T, batch = BATCH)
    public static void station_hero(GameTestHelper h) {
        FakePlayer p = player(h, "pt59");
        PlaytestSetup.reset(p, PlaytestState.get(h.getLevel().getServer()));
        PlaytestSetup.apply(p, spec("hero"));
        PlayerMythData d = MythData.get(p);
        h.assertTrue(d.firstKills().contains("mythic_age:minotaur"), "首杀：" + d.firstKills());
        h.assertTrue(d.civ(GREEK).vouchers(Age.CLASSICAL) == 1, "古典凭证 1：" + d.civ(GREEK).vouchers(Age.CLASSICAL));
        h.assertTrue(d.blueprints().contains("greek:harpe"), "图纸：" + d.blueprints());
        h.assertTrue(BestiaryRules.tier(d.discoveries(), "greek/minotaur") == BestiaryRules.Tier.DEFEATED, "图鉴击败");
        h.assertTrue(d.civ(GREEK).favor() == 100, "恩宠 100：" + d.civ(GREEK).favor());
        h.assertTrue(count(p, "mythic_age:bronze_ingot") == 16 && p.getInventory().countItem(Items.GOLD_INGOT) == 8
                && p.getInventory().countItem(Items.COOKED_MUTTON) == 16 && p.getInventory().countItem(Items.HONEY_BOTTLE) == 4,
                "英雄供品齐");
        cleanup(h, p);
        h.succeed();
    }

    // ───────────── 神庙（GA-13：神庙扩建站与「至少几级」的准备见 VillageTempleGameTests） ─────────────

    // ───────────── 任务与记录 ─────────────

    /** TC-GA3-63、64：同时两次进站第二次被拒；假玩家不在线，下一刻任务取消。 */
    @GameTest(template = T, batch = BATCH)
    public static void job_busy_then_cancelled(GameTestHelper h) {
        FakePlayer p = player(h, "pt63");
        PlaytestStations.Station s = PlaytestStations.byId("start").orElseThrow();
        h.assertTrue(PlaytestJobs.begin(p, s), "第一次开始");
        h.assertFalse(PlaytestJobs.begin(p, s), "第二次被拒");
        h.succeedWhen(() -> h.assertFalse(PlaytestJobs.running(p.getUUID()), "不在线的玩家任务应取消"));
    }

    /** TC-GA3-65：记录文件依次有站点开始、受祝福、神庙等级，同一里程碑只记一次，结束有用时。 */
    @GameTest(template = T, batch = BATCH)
    public static void log_milestones(GameTestHelper h) throws IOException {
        FakePlayer p = player(h, "pt65");
        Path tmp = Files.createTempFile("mythic_age_playtest", ".md");
        AtomicLong now = new AtomicLong(1_000_000);
        Path old = PlaytestLog.file();
        PlaytestLog.overrideForTest(tmp, now::get);
        try {
            PlaytestLog.start(p, PlaytestStations.byId("temple").orElseThrow());
            // 静默期内（准备时的扫描回声）不记
            MythEvents.fire(p, MythEvents.TEMPLE_LEVEL, GREEK, 3);
            PlaytestLog.unquietForTest(p.getUUID());
            MythEvents.fire(p, MythEvents.BLESSED, GREEK);
            MythEvents.fire(p, MythEvents.BLESSED, GREEK);
            MythEvents.fire(p, MythEvents.TEMPLE_LEVEL, GREEK, 1);
            now.addAndGet(612_000);
            h.assertTrue(PlaytestLog.done(p).isPresent(), "结束");
            String text = Files.readString(tmp);
            h.assertTrue(text.contains("第 2 站 temple（开始）"), "站点开始：" + text);
            h.assertTrue(text.indexOf("受祝福") == text.lastIndexOf("受祝福") && text.contains("受祝福"), "受祝福只记一次：" + text);
            h.assertTrue(text.indexOf("受祝福") < text.indexOf("神庙认定 1 级"), "顺序：" + text);
            h.assertTrue(text.contains("用时 10 分 12 秒"), "用时：" + text);
            h.assertFalse(text.contains("神庙认定 3 级"), "静默期内不记：" + text);
            // 不在站里时事件不记
            MythEvents.fire(p, MythEvents.TEMPLE_LEVEL, GREEK, 2);
            h.assertFalse(Files.readString(tmp).contains("神庙认定 2 级"), "站外不记");
        } finally {
            PlaytestLog.overrideForTest(old, System::currentTimeMillis);
            Files.deleteIfExists(tmp);
        }
        h.succeed();
    }

    /** TC-GA3-66：交付委托完成后触发一次 quest_done。 */
    @GameTest(template = T, batch = "quest_supply")
    public static void quest_done_event(GameTestHelper h) {
        VillageWorkGameTests.TV tv = QuestGameTests.village(h);
        QuestGameTests.project(h, tv, "minecraft:copper_ingot", 6, QuestGameTests.today(h));
        FakePlayer a = QuestGameTests.player(h, "pt66", 8, 12);
        a.getInventory().add(new ItemStack(Items.COPPER_INGOT, 10));
        List<String> events = new ArrayList<>();
        BiConsumer<net.minecraft.server.level.ServerPlayer, MythEvents.Fired> l = (sp, f) -> {
            if (f.player().equals(a.getUUID())) {
                events.add(f.event());
            }
        };
        MythEvents.addListener(l);
        try {
            QuestGameTests.scan(h, tv);
            Quest q = QuestGameTests.only(h, tv, "supply:project:");
            QuestService.Outcome o = QuestGameTests.act(a, tv, q, QuestService.DELIVER);
            h.assertTrue(o.ok() && q.state == QuestState.SETTLED, "完成并结算：" + o + " " + q.state);
        } finally {
            MythEvents.removeListener(l);
        }
        h.assertTrue(events.stream().filter(MythEvents.QUEST_DONE::equals).count() == 1, "quest_done 一次：" + events);
        QuestGameTests.cleanup(h, tv, a);
        h.succeed();
    }
}
