package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.map.MapDelta;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.relic.OfferedIndex;
import io.github.happysnappingturtle.mythicage.relic.RelicService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.ConstructionSignBlock;
import io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads;
import io.github.happysnappingturtle.mythicage.village.build.SiteMarkers;
import io.github.happysnappingturtle.mythicage.village.build.VillageProjects;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.map.VillageMapItems;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GA-12 村庄界面（TC-GA12-20～29）：场地与村庄照 {@link VillageProjectGameTests#village}（古典希腊村，小屋 + 谷仓，整个场地算村界）。
 * 假玩家收不到网络包，用 {@link VillageScreenService#trackForTest} 记下发给它的包。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageScreenGameTests {
    private static final String BATCH = "village_screen";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String HOUSE = "greek:house_small", HOUSE_II = "greek:house_small_ii";
    private static final String GRANARY_II = "greek:granary_ii";

    private VillageScreenGameTests() {
    }

    // ───────────── 搭建 ─────────────

    /** 站在村中心旁、被记包的假玩家；贡献 contribution。 */
    private static FakePlayer viewer(GameTestHelper h, VillageProjectGameTests.Site site, int contribution) {
        FakePlayer p = VillageProjectGameTests.player(h, site, contribution);
        VillageScreenService.trackForTest(p);
        return p;
    }

    private static void cleanup(GameTestHelper h, VillageProjectGameTests.Site site) {
        site.players().forEach(VillageScreenService::untrackForTest);
        VillageProjectGameTests.cleanup(h, site);
    }

    private static <T extends CustomPacketPayload> List<T> sent(FakePlayer p, Class<T> type) {
        return VillageScreenService.sentForTest(p.getUUID()).stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** 收到的「打开」包：只能有一个，分页是 tab。 */
    private static void assertOpened(GameTestHelper h, FakePlayer p, VillageProjectGameTests.Site site, int tab) {
        List<VillageScreenPayloads.Opened> opened = sent(p, VillageScreenPayloads.Opened.class);
        h.assertTrue(opened.size() == 1 && opened.get(0).village().equals(site.tv().id()) && opened.get(0).tab() == tab,
                "打开到分页 " + tab + "：" + opened);
        h.assertTrue(VillageScreenService.openVillage(p.getUUID()).equals(Optional.of(site.tv().id())), "服务端记下会话");
    }

    /** 收到地图全量，解出来有小屋与谷仓。 */
    private static void assertFullMap(GameTestHelper h, FakePlayer p) {
        List<VillageScreenPayloads.MapPacket> maps = sent(p, VillageScreenPayloads.MapPacket.class);
        h.assertTrue(!maps.isEmpty() && maps.get(0).full(), "先收到地图全量：" + maps.size());
        VillageMapData m = VillageMapData.decode(maps.get(0).data());
        h.assertTrue(m.buildings().stream().anyMatch(b -> b.building().equals(HOUSE)), "地图上有小屋：" + m.buildings());
        h.assertTrue(m.buildings().stream().anyMatch(b -> b.building().equals("greek:granary")), "地图上有谷仓：" + m.buildings());
    }

    /** 开小屋升级、不备料（只开工）。 */
    private static BuildProject startHouseUpgrade(GameTestHelper h, VillageProjectGameTests.Site site) {
        VillageContext ctx = VillageProjectGameTests.ctx(h, site);
        return Construction.start(ctx, new ProjectPicker.Pick(HOUSE_II, "", "", HOUSE, site.house().origin())).orElseThrow();
    }

    /** 逐格推进，直到地图上的进度百分比比 from 大（最多 400 步）。 */
    private static int advance(GameTestHelper h, VillageProjectGameTests.Site site, BuildProject p, int from) {
        int now = from;
        for (int i = 0; i < 400 && now <= from && p.active(); i++) {
            VillageProjectGameTests.phasedStep(h, site, p);
            now = io.github.happysnappingturtle.mythicage.village.map.VillageMaps.progress(VillageProjectGameTests.ctx(h, site), p);
        }
        return now;
    }

    // ───────────── 入口 ─────────────

    /** TC-GA12-20：右键谷仓管事 → 打开到地图页，收到状态栏与地图全量。 */
    @GameTest(template = T, batch = BATCH)
    public static void keeper_opens_map(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        try {
            FakePlayer p = viewer(h, site, 0);
            BlockPos at = h.absolutePos(new BlockPos(13, 2, 16));
            ResidentEntity keeper = Residents.spawn(h.getLevel(), site.tv().village(), VillageData.civ("greek").orElseThrow(), "greek:laborer",
                    at, at, true).orElseThrow();
            keeper.setNoAi(true);
            VillageAffairs.onResidentInteract(p, keeper);
            assertOpened(h, p, site, VillageScreenPayloads.TAB_MAP);
            assertFullMap(h, p);
            h.assertTrue(!sent(p, VillageScreenPayloads.People.class).isEmpty() && !sent(p, VillageScreenPayloads.Faith.class).isEmpty()
                    && !sent(p, ProjectPayloads.View.class).isEmpty(), "各分页的数据一起发了");
            h.assertTrue(sent(p, VillageScreenPayloads.Opened.class).get(0).header().atKeeper(), "状态栏记着在管事身边（能交付、捐献）");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA12-21：和本村祭司说话时，「从祭司打开」到信仰页；没有祭司会话时不打开。 */
    @GameTest(template = T, batch = BATCH)
    public static void priest_opens_faith(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        GuardianEntity g = null;
        try {
            FakePlayer p = viewer(h, site, 0);
            h.assertFalse(VillageScreenService.openRequested(p, VillageScreenPayloads.FROM_PRIEST, VillageScreenPayloads.TAB_FAITH),
                    "没有祭司会话不打开");
            BlockPos bell = site.tv().village().bell().pos();
            g = GuardianSpawner.spawnNew(h.getLevel(), bell.offset(1, 0, 2), Optional.of(bell), Civilization.GREEK);
            h.assertTrue(g != null, "祭司生成");
            g.setNoAi(true);
            h.assertTrue(TradeService.openGuardian(p, g), "和祭司说话");
            h.assertTrue(VillageScreenService.openRequested(p, VillageScreenPayloads.FROM_PRIEST, VillageScreenPayloads.TAB_FAITH),
                    "从祭司打开");
            assertOpened(h, p, site, VillageScreenPayloads.TAB_FAITH);
            VillageScreenPayloads.Faith f = sent(p, VillageScreenPayloads.Faith.class).get(0);
            h.assertTrue(f.village().equals(site.tv().id()) && f.present(), "信仰页有本村神庙：" + f);
            h.succeed();
        } finally {
            site.players().forEach(TradeService::close);
            if (g != null) {
                g.discard();
            }
            cleanup(h, site);
        }
    }

    /**
     * TC-GA12-22：右键施工牌 → 打开到建设页；建设页数据里有逐格进度。
     * 施工牌受全服每刻放块配额限制（同批并行的用例会先用掉），逐刻推进直到放下。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void sign_opens_build(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        BuildProject proj = VillageProjectGameTests.upgrade(h, site, HOUSE_II, HOUSE, site.house());
        BlockPos[] sign = new BlockPos[1];
        h.startSequence().thenWaitUntil(() -> {
            VillageContext ctx = VillageProjectGameTests.ctx(h, site);
            Construction.tick(ctx);
            sign[0] = SiteMarkers.sign(ctx, proj).orElse(null);
            h.assertTrue(sign[0] != null, "等施工牌放下");
        }).thenExecute(() -> {
            try {
                h.assertTrue(h.getLevel().getBlockState(sign[0]).is(ConstructionSignBlock.SIGN.get()), "施工牌在世界里");
                FakePlayer p = viewer(h, site, 0);
                h.getLevel().getBlockState(sign[0]).useWithoutItem(h.getLevel(), p,
                        new BlockHitResult(Vec3.atCenterOf(sign[0]), Direction.UP, sign[0], false));
                assertOpened(h, p, site, VillageScreenPayloads.TAB_BUILD);
                ProjectPayloads.View v = sent(p, ProjectPayloads.View.class).get(0);
                h.assertTrue(v.total() > 0 && !v.current().isEmpty(), "建设页有当前项目与总格数：" + v.total());
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }

    /** TC-GA12-23：村界内手持已填充地图潜行右键 → 打开地图页，地图加上村庄标记（再右键不重复加）。 */
    @GameTest(template = T, batch = BATCH)
    public static void map_item_inside_marks(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        try {
            FakePlayer p = viewer(h, site, 0);
            ItemStack map = MapItem.create(h.getLevel(), (int) p.getX(), (int) p.getZ(), (byte) 0, true, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, map);
            p.setShiftKeyDown(true);
            var event = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
            VillageMapItems.onRightClickItem(event);
            h.assertTrue(event.isCanceled(), "接管了右键（不打开原版地图）");
            assertOpened(h, p, site, VillageScreenPayloads.TAB_MAP);
            h.assertTrue(VillageMapItems.marked(p.getMainHandItem()).size() == 1, "地图记下一个村子");
            h.assertTrue(p.getMainHandItem().get(net.minecraft.core.component.DataComponents.MAP_DECORATIONS) != null
                    && p.getMainHandItem().get(net.minecraft.core.component.DataComponents.MAP_DECORATIONS).decorations().keySet().stream()
                    .anyMatch(k -> k.startsWith(VillageMapItems.PREFIX)), "地图上有村庄标记");
            h.assertFalse(VillageMapItems.mark(p.getMainHandItem(), site.tv().village()), "再标一次不重复加");
            h.assertTrue(VillageMapItems.marked(p.getMainHandItem()).size() == 1, "仍是一个");
            p.setShiftKeyDown(false);
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA12-24：村界外手持地图潜行右键 → 不打开、不加标记、不接管右键。 */
    @GameTest(template = T, batch = BATCH)
    public static void map_item_outside_ignored(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        try {
            FakePlayer p = viewer(h, site, 0);
            p.moveTo(p.getX() + 5000, p.getY(), p.getZ());
            ItemStack map = MapItem.create(h.getLevel(), (int) p.getX(), (int) p.getZ(), (byte) 0, true, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, map);
            p.setShiftKeyDown(true);
            var event = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
            VillageMapItems.onRightClickItem(event);
            p.setShiftKeyDown(false);
            h.assertFalse(event.isCanceled(), "不接管");
            h.assertTrue(sent(p, VillageScreenPayloads.Opened.class).isEmpty() && !VillageScreenService.isOpen(p.getUUID()), "不打开");
            h.assertTrue(VillageMapItems.marked(p.getMainHandItem()).isEmpty(), "不加标记");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    // ───────────── 增量 ─────────────

    /** TC-GA12-25：界面开着时施工推进 → 每 20 刻收到地图增量（不是全量），里面是在建的小屋、进度变大了。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void open_screen_gets_progress_delta(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        BuildProject proj = VillageProjectGameTests.upgrade(h, site, HOUSE_II, HOUSE, site.house());
        FakePlayer p = viewer(h, site, 0);
        h.assertTrue(VillageScreenService.open(p, site.tv().id(), VillageScreenPayloads.TAB_MAP), "打开");
        int before = io.github.happysnappingturtle.mythicage.village.map.VillageMaps.progress(VillageProjectGameTests.ctx(h, site), proj);
        int[] after = new int[1];
        h.startSequence().thenIdle(25).thenExecute(() -> {
            VillageScreenService.clearSentForTest(p.getUUID());
            after[0] = advance(h, site, proj, before);
        }).thenIdle(45).thenExecute(() -> {
            try {
                h.assertTrue(after[0] > before, "进度变了：" + before + " → " + after[0]);
                List<VillageScreenPayloads.MapPacket> maps = sent(p, VillageScreenPayloads.MapPacket.class);
                h.assertTrue(!maps.isEmpty() && maps.stream().noneMatch(VillageScreenPayloads.MapPacket::full), "只发增量：" + maps.size());
                var m = maps.get(0);
                MapDelta d = MapDelta.decode(m.data(), m.centerX(), m.centerZ());
                h.assertTrue(d.changed().stream().anyMatch(b -> b.underConstruction() && b.progress() > before),
                        "增量里是在建建筑的新进度：" + d);
                h.assertTrue(VillageScreenService.sessionMapForTest(p.getUUID()).flatMap(x -> x.buildings().stream()
                        .filter(VillageMapData.Building::underConstruction).findFirst()).map(b -> b.progress() == after[0]).orElse(false),
                        "服务端会话里的地图跟上了");
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }

    /** TC-GA12-29：关掉界面 → 服务端停发增量；施工照样推进。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void closed_screen_stops_updates(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        BuildProject proj = VillageProjectGameTests.upgrade(h, site, HOUSE_II, HOUSE, site.house());
        FakePlayer p = viewer(h, site, 0);
        h.assertTrue(VillageScreenService.open(p, site.tv().id(), VillageScreenPayloads.TAB_BUILD), "打开");
        VillageScreenService.closed(p, site.tv().id());
        h.assertFalse(VillageScreenService.isOpen(p.getUUID()), "会话已关");
        VillageScreenService.clearSentForTest(p.getUUID());
        int before = io.github.happysnappingturtle.mythicage.village.map.VillageMaps.progress(VillageProjectGameTests.ctx(h, site), proj);
        int after = advance(h, site, proj, before);
        h.startSequence().thenIdle(45).thenExecute(() -> {
            try {
                h.assertTrue(after > before, "施工照样推进：" + before + " → " + after);
                h.assertTrue(sent(p, VillageScreenPayloads.MapPacket.class).isEmpty()
                        && sent(p, VillageScreenPayloads.HeaderUpdate.class).isEmpty(), "不再发地图与状态栏");
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }

    // ───────────── 操作 ─────────────

    /** TC-GA12-26：建设页「点名」与命令走同一条路：贡献够进「下一个」位；「清空」清掉；贡献不够被拒、什么都不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_page_nominate(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        try {
            VillageProjectGameTests.upgrade(h, site, HOUSE_II, HOUSE, site.house());
            FakePlayer low = viewer(h, site, 10);
            var refused = ProjectPayloads.nominate(low, new ProjectPayloads.Nominate(site.tv().id(), GRANARY_II)).orElseThrow();
            h.assertTrue(refused.result() == VillageProjects.Result.LOW_CONTRIBUTION, "贡献 10 被拒：" + refused);
            h.assertTrue(VillageProjectGameTests.ctx(h, site).state().nextProject == null, "拒绝后「下一个」位空着");
            FakePlayer p = viewer(h, site, 64);
            var ok = ProjectPayloads.nominate(p, new ProjectPayloads.Nominate(site.tv().id(), GRANARY_II)).orElseThrow();
            h.assertTrue(ok.result() == VillageProjects.Result.OK, "点名：" + ok);
            var s = VillageProjectGameTests.ctx(h, site).state();
            h.assertTrue(s.nextProject != null && s.nextProject.key().equals(GRANARY_II) && s.nextProject.player().equals(p.getUUID()),
                    "进「下一个」位：" + s.nextProject);
            h.assertTrue(s.log.stream().anyMatch(l -> l.key().equals("mythic_age.village.log.project_nominated")), "发展记录");
            var cleared = ProjectPayloads.nominate(p, new ProjectPayloads.Nominate(site.tv().id(), "")).orElseThrow();
            h.assertTrue(cleared.result() == VillageProjects.Result.CLEARED && s.nextProject == null, "清空");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA12-27：「捐出背包里的」只捐当前项目正好缺的：多带的留在背包，别的物品不动，记贡献。 */
    @GameTest(template = T, batch = BATCH)
    public static void donate_missing_only(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        try {
            BuildProject proj = startHouseUpgrade(h, site);
            VillageContext ctx = VillageProjectGameTests.ctx(h, site);
            Construction.tick(ctx);
            BuildCosts costs = ctx.data().buildCosts();
            Map.Entry<String, Integer> want = proj.missing().entrySet().stream()
                    .filter(e -> e.getValue() <= 128 && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(costs.representative(e.getKey()))))
                    .findFirst().orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("没有缺料：" + proj.missing()));
            String item = costs.representative(want.getKey());
            int need = want.getValue() - costs.items(want.getKey()).stream().mapToInt(ctx.state().stock::count).sum();
            h.assertTrue(need > 0, "项目缺 " + item + "：" + want);
            var it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));
            FakePlayer p = viewer(h, site, 0);
            int carry = need + 5;
            for (int left = carry; left > 0; left -= Math.min(left, it.getDefaultMaxStackSize())) {
                p.getInventory().add(new ItemStack(it, Math.min(left, it.getDefaultMaxStackSize())));
            }
            p.getInventory().add(new ItemStack(Items.DIAMOND, 3));
            int stockBefore = ctx.state().stock.count(item);
            int contribBefore = VillageProjects.contribution(p, ctx);
            VillageScreenService.act(p, new VillageScreenPayloads.Act(site.tv().id(), VillageScreenPayloads.Act.DONATE_MISSING, 0));
            h.assertTrue(p.getInventory().countItem(it) == 5, "多带的 5 件留下：" + p.getInventory().countItem(it));
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 3, "别的物品不动");
            h.assertTrue(ctx.state().stock.count(item) == stockBefore + need, "村库多了正好缺的 " + need);
            h.assertTrue(VillageProjects.contribution(p, ctx) > contribBefore, "记贡献");
            // 再捐一次：已经不缺了，什么都不捐
            VillageScreenService.act(p, new VillageScreenPayloads.Act(site.tv().id(), VillageScreenPayloads.Act.DONATE_MISSING, 0));
            h.assertTrue(p.getInventory().countItem(it) == 5, "不缺就不再捐");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA12-28：信仰页放入、取回圣物，与潜行右键祭坛的效果相同（供奉位、绑定状态、背包）。 */
    @GameTest(template = T, batch = BATCH)
    public static void faith_page_relic_offer_retrieve(GameTestHelper h) {
        VillageProjectGameTests.Site site = VillageProjectGameTests.village(h);
        BlockPos altar = site.tv().village().bell().pos();
        FakePlayer p = null;
        try {
            h.getLevel().setBlockAndUpdate(altar, GreekVillageBlocks.VILLAGE_ALTAR.get().defaultBlockState());
            p = viewer(h, site, 0);
            p.moveTo(Vec3.atBottomCenterOf(altar.offset(-2, 0, 0)));
            MythData.get(p).civ(Civilization.GREEK).setAge(Age.ARCHAIC);
            RelicService.onlineForTest(p, true);
            VillageTemples.invalidate(site.tv().id());
            VillageTemples.addDevotee(h.getLevel().getServer(), site.tv().id(), p.getUUID(), 0, TempleBook.Via.JOIN);
            VillageTemple temple = VillageTemples.of(h.getLevel().getServer(), site.tv().id()).orElseThrow();
            h.assertTrue(temple.altar().pos().equals(altar), "神庙祭坛在钟的位置：" + temple.altar());
            VillageScreenService.open(p, site.tv().id(), VillageScreenPayloads.TAB_FAITH);
            VillageScreenService.clearSentForTest(p.getUUID());

            // 手里没有圣物：不放、提示
            VillageScreenService.act(p, new VillageScreenPayloads.Act(site.tv().id(), VillageScreenPayloads.Act.RELIC_OFFER, 0));
            h.assertTrue(VillageTemples.book(h.getLevel().getServer(), site.tv().id()).relics(p.getUUID()).isEmpty(), "空手不放");

            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GreekRelics.RHEIA_CROWN.get()));
            VillageScreenService.act(p, new VillageScreenPayloads.Act(site.tv().id(), VillageScreenPayloads.Act.RELIC_OFFER, 0));
            h.assertTrue(p.getMainHandItem().isEmpty(), "放进去了");
            h.assertTrue(VillageTemples.book(h.getLevel().getServer(), site.tv().id()).relics(p.getUUID()).size() == 1, "供奉位一件");
            h.assertTrue(MythData.get(p).relics().state("greek:rheia_crown") == RelicState.OFFERED, "绑定为已供奉");
            List<VillageScreenPayloads.Faith> faith = sent(p, VillageScreenPayloads.Faith.class);
            h.assertTrue(!faith.isEmpty() && faith.get(faith.size() - 1).relics().size() == 1, "信仰页刷新，显示这件圣物");

            VillageScreenService.act(p, new VillageScreenPayloads.Act(site.tv().id(), VillageScreenPayloads.Act.RELIC_RETRIEVE, 0));
            h.assertTrue(p.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) == 1, "取回到背包");
            h.assertTrue(VillageTemples.book(h.getLevel().getServer(), site.tv().id()).relics(p.getUUID()).isEmpty(), "供奉位空了");
            h.assertTrue(MythData.get(p).relics().state("greek:rheia_crown") == RelicState.INACTIVE, "绑定删除");
            h.succeed();
        } finally {
            OfferedIndex.removeVillage(site.tv().id());
            VillageTemples.invalidate(site.tv().id());
            if (p != null) {
                RelicService.onlineForTest(p, false);
            }
            h.getLevel().removeBlock(altar, false);
            cleanup(h, site);
        }
    }
}
