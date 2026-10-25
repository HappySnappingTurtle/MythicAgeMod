package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.content.greek.village.GreekFoundation;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.village.FoundationService;
import io.github.happysnappingturtle.mythicage.village.VillageAnchor;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.build.AdHocSites;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlockEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.ResidentMigration;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * T19 阶段 ⑥：奠基石与临时找地（单元测试用例文档 TC-T19-135～144）。立村要求 32 格内没有别的村庄，
 * 并排放的用例会互相挡住，所以每个用例一个批次。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoundationGameTests {
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final BlockPos STONE = new BlockPos(8, 2, 18);

    private FoundationGameTests() {
    }

    static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 24))));
        OnlinePlayers.forTest(p, true);
        return p;
    }

    /** 放一块城邦奠基石（走放下后的流程：记建立者、能恢复就恢复）。 */
    static FoundationStoneBlockEntity place(GameTestHelper h, FakePlayer p, BlockPos rel) {
        h.setBlock(rel, GreekFoundation.HEARTH_STONE.get());
        BlockPos abs = h.absolutePos(rel);
        FoundationStoneBlockEntity be = (FoundationStoneBlockEntity) h.getLevel().getBlockEntity(abs);
        FoundationService.onPlaced(h.getLevel(), abs, p, be, Civilization.GREEK);
        return be;
    }

    static VillageRegistry.Village founded(GameTestHelper h, FakePlayer p) {
        VillageEconomy.get(h.getLevel().getServer());
        FoundationStoneBlockEntity be = place(h, p, STONE);
        Optional<net.minecraft.network.chat.Component> err = FoundationService.found(h.getLevel(), p, h.absolutePos(STONE), "zeus");
        h.assertTrue(err.isEmpty(), "立村：" + err.map(net.minecraft.network.chat.Component::getString).orElse(""));
        return VillageRegistry.get(h.getLevel().getServer()).byId(be.village()).orElseThrow();
    }

    /** 清掉本用例立的村庄（登记、规划、经济、居民、祭司）。 */
    static void cleanup(GameTestHelper h, VillageRegistry.Village v, FakePlayer... players) {
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        reg.byId(v.id()).ifPresent(x -> FoundationService.expire(h.getLevel(), x));
        reg.byId(v.id()).ifPresent(x -> reg.remove(x.bell()));
        for (FakePlayer p : players) {
            OnlinePlayers.forTest(p, false);
        }
        h.getLevel().getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(40))
                .forEach(Villager::discard);
    }

    /** TC-T19-135 + 142：空地立村——FOUNDED、锚点种类 FOUNDATION、建立者、结识主神；没有床时有 3 个露宿位，移民睡在石灶旁。 */
    @GameTest(template = T, batch = "foundation_found")
    public static void found_on_open_ground(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        FakePlayer p = player(h, "founder135");
        VillageRegistry.Village v = founded(h, p);
        try {
            h.assertTrue(v.origin() == VillageRegistry.Origin.FOUNDED && v.anchor().kind() == VillageAnchor.Kind.FOUNDATION,
                    "来源与锚点：" + v.origin());
            h.assertTrue(p.getUUID().equals(v.founder()) && v.patron().equals("zeus") && v.civ().equals("greek"), "建立者与主神");
            h.assertTrue(v.bell().pos().equals(h.absolutePos(STONE)), "钟坐标 = 奠基石");
            h.assertTrue(MythData.get(p).civ(Civilization.GREEK).knownPatrons().contains("zeus"), "自动结识宙斯");
            VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(v.id()).orElseThrow();
            h.assertTrue(plan.anchors(FoundationService.CAMP).size() == 3, "3 个露宿位");
            h.assertTrue(GuardianRegistry.get(h.getLevel().getServer()).byBell(h.getLevel().dimension(), v.bell().pos()).isPresent(),
                    "石灶旁有祭司");
            VillageScheduler.forceRunningForTest(v.id(), true);
            VillageScheduler.processNowForTest(h.getLevel(), v.id());
            var ctx = VillageScheduler.contextForTest(h.getLevel(), v.id()).orElseThrow();
            h.assertTrue(Residents.freeHomes(ctx.plan(), ctx.state()).size() == 3, "露宿位当床位");
            var m = ResidentMigration.forceForCommand(ctx);
            h.assertTrue(m.isPresent() && plan.anchors(FoundationService.CAMP).stream()
                    .anyMatch(c -> c.x() == m.get().home().getX() && c.z() == m.get().home().getZ()), "移民分到露宿位");
            h.succeed();
        } finally {
            cleanup(h, v, p);
        }
    }

    /** TC-T19-136：32 格内有别的村庄的已建范围，不能放。 */
    @GameTest(template = T, batch = "foundation_close")
    public static void too_close_to_other_village(GameTestHelper h) {
        VillageWorkGameTests.TV other = VillageMigrationGameTests.village(h, 0, Age.ARCHAIC, java.util.Map.of(), java.util.List.of());
        FakePlayer p = player(h, "founder136");
        try {
            h.assertTrue(FoundationService.placementError(p, h.getLevel(), h.absolutePos(STONE), Civilization.GREEK).isPresent(),
                    "旁边有村庄不能放");
            h.succeed();
        } finally {
            OnlinePlayers.forTest(p, false);
            VillageWorkGameTests.cleanup(h, other);
        }
    }

    /** TC-T19-137 + 140（后半）：在中立原版村庄的钟旁立村——该村立为希腊，原版村民成为信徒；宽限过期后回到中立。 */
    @GameTest(template = T, batch = "foundation_vanilla")
    public static void found_on_neutral_vanilla_village(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        BlockPos bellRel = new BlockPos(8, 2, 10);
        h.setBlock(bellRel, Blocks.BELL);
        GlobalPos bell = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(bellRel));
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        VillageRegistry.Village neutral = new VillageRegistry.Village(UUID.randomUUID(), bell, "", "", VillageRegistry.Origin.VANILLA,
                null, "plains", -1);
        reg.add(neutral);
        Villager villager = h.spawn(EntityType.VILLAGER, new BlockPos(6, 2, 12));
        villager.getBrain().setMemory(MemoryModuleType.MEETING_POINT, bell);
        h.assertTrue(BelieverEvents.greekVillage(villager).isEmpty(), "立村前是中立村民");
        FakePlayer p = player(h, "founder137");
        VillageRegistry.Village v = founded(h, p);
        try {
            h.assertTrue(v.id().equals(neutral.id()) && v.bell().equals(bell), "就是那座原版村庄，钟不变");
            h.assertTrue(v.civ().equals("greek") && v.origin() == VillageRegistry.Origin.FOUNDED, "立为希腊");
            h.assertTrue(BelieverEvents.greekVillage(villager).isPresent(), "原版村民成为希腊信徒");
            // 宽限过期：拆了 4 日
            h.getLevel().removeBlock(h.absolutePos(STONE), false);
            long today = MythDays.today(h.getLevel().getServer());
            VillageRegistry.Village gone = reg.byId(v.id()).orElseThrow();
            h.assertTrue(gone.missingSinceDay() == today, "拆了进入宽限");
            MythDays.advanceForTest(4);
            try {
                VillageService.checkBells(h.getLevel(), h.absolutePos(STONE));
            } finally {
                MythDays.advanceForTest(-4);
            }
            VillageRegistry.Village back = reg.byId(v.id()).orElseThrow();
            h.assertTrue(back.neutral() && back.origin() == VillageRegistry.Origin.VANILLA, "回到中立原版村庄：" + back.origin() + " "
                    + back.civ() + " 缺 " + back.missingSinceDay() + " 今天 " + today);
            h.assertTrue(VillagePlans.get(h.getLevel().getServer()).byVillage(v.id()).isEmpty(), "规划数据删掉");
            h.assertTrue(BelieverEvents.greekVillage(villager).isEmpty() && villager.isAlive(), "村民回到中立、还在");
            h.succeed();
        } finally {
            cleanup(h, v, p);
        }
    }

    /** TC-T19-138：已有 2 个（含宽限中的）不能再立。 */
    @GameTest(template = T, batch = "foundation_limit")
    public static void limit_two_per_civ(GameTestHelper h) {
        FakePlayer p = player(h, "founder138");
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        VillageRegistry.Village a = new VillageRegistry.Village(UUID.randomUUID(),
                GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(5000, 2, 0))), "greek", "zeus",
                VillageRegistry.Origin.FOUNDED, p.getUUID(), "", -1);
        VillageRegistry.Village b = new VillageRegistry.Village(UUID.randomUUID(),
                GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(6000, 2, 0))), "greek", "hades",
                VillageRegistry.Origin.FOUNDED, p.getUUID(), "", MythDays.today(h.getLevel().getServer()));
        reg.add(a);
        reg.add(b);
        try {
            var err = FoundationService.placementError(p, h.getLevel(), h.absolutePos(STONE), Civilization.GREEK);
            h.assertTrue(err.isPresent() && err.get().getString().contains("2"), "第 3 个拒绝（宽限中的也算）：" + err);
            reg.remove(b.bell());
            h.assertTrue(FoundationService.placementError(p, h.getLevel(), h.absolutePos(STONE), Civilization.GREEK).isEmpty(),
                    "只剩 1 个就能放");
            h.succeed();
        } finally {
            reg.remove(a.bell());
            reg.remove(b.bell());
            OnlinePlayers.forTest(p, false);
        }
    }

    /** TC-T19-139 + 140（前半）+ 144：非建立者拆不掉；建立者拆后 2 日放回原处恢复；待选主神的奠基石不立村。 */
    @GameTest(template = T, batch = "foundation_break")
    public static void break_and_restore(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        FakePlayer p = player(h, "founder139");
        FakePlayer other = player(h, "stranger139");
        // 待选：放下没选
        FoundationStoneBlockEntity pending = place(h, p, new BlockPos(2, 2, 2));
        h.assertTrue(pending.pending() && p.getUUID().equals(pending.owner()), "待选主神");
        h.assertTrue(FoundationService.found(h.getLevel(), other, h.absolutePos(new BlockPos(2, 2, 2)), "zeus").isPresent(),
                "别人不能替他选");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        VillageRegistry.Village v = founded(h, p);
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        try {
            BlockPos abs = h.absolutePos(STONE);
            BlockEvent.BreakEvent e = new BlockEvent.BreakEvent(h.getLevel(), abs, h.getLevel().getBlockState(abs), other);
            FoundationService.onBreak(e);
            h.assertTrue(e.isCanceled(), "非建立者拆不掉");
            BlockEvent.BreakEvent mine = new BlockEvent.BreakEvent(h.getLevel(), abs, h.getLevel().getBlockState(abs), p);
            FoundationService.onBreak(mine);
            h.assertTrue(!mine.isCanceled(), "建立者能拆");
            h.getLevel().removeBlock(abs, false);
            long today = MythDays.today(h.getLevel().getServer());
            h.assertTrue(reg.byId(v.id()).orElseThrow().missingSinceDay() == today, "拆了进入宽限");
            FoundationStoneBlockEntity again;
            MythDays.advanceForTest(2);
            try {
                again = place(h, p, STONE);
            } finally {
                MythDays.advanceForTest(-2);
            }
            h.assertTrue(again.village() != null && again.village().equals(v.id()), "2 日内放回原处恢复：" + again.village() + " / "
                    + reg.byId(v.id()).map(x -> x.missingSinceDay() + " 今天 " + today + " " + x.founder() + " " + x.civ() + " 石 "
                    + FoundationService.stonePos(h.getLevel(), x)).orElse("无") + " 放在 " + abs);
            h.assertTrue(reg.byId(v.id()).orElseThrow().missingSinceDay() < 0, "宽限清除");
            h.assertTrue(FoundationService.present(h.getLevel(), reg.byId(v.id()).orElseThrow()), "村庄在");
            h.succeed();
        } finally {
            cleanup(h, v, p, other);
        }
    }

    /** TC-T19-141：奠基村庄的第一个项目是起步住居，用临时找地划出地块（追加进规划数据）。 */
    @GameTest(template = T, batch = "foundation_starter", timeoutTicks = 200)
    public static void first_project_is_starter_house(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        FakePlayer p = player(h, "founder141");
        VillageRegistry.Village v = founded(h, p);
        try {
            VillageScheduler.forceRunningForTest(v.id(), true);
            VillageScheduler.processNowForTest(h.getLevel(), v.id());
            var ctx = VillageScheduler.contextForTest(h.getLevel(), v.id()).orElseThrow();
            Optional<BuildProject> pr = ctx.state().activeProject().isPresent() ? ctx.state().activeProject() : Construction.daily(ctx);
            h.assertTrue(pr.isPresent(), "开工了：缺口 " + VillageScheduler.runtime(v.id()).needsSpace());
            h.assertTrue(pr.get().building.equals("greek:house_small") && pr.get().plot.startsWith(AdHocSites.PREFIX),
                    "起步住居在临时地块：" + pr.get().building + " @ " + pr.get().plot);
            VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(v.id()).orElseThrow();
            Box plot = plan.info().plots().stream().filter(x -> x.id().equals(pr.get().plot)).findFirst().orElseThrow().box();
            Box core = plan.info().built().get(0);
            double gap = io.github.happysnappingturtle.mythicage.core.village.found.FoundationRules.distanceXZ(core,
                    (plot.minX() + plot.maxX()) / 2, (plot.minZ() + plot.maxZ()) / 2);
            h.assertTrue(gap >= 8 && gap <= 40, "离村 8–32 格（地块中心 " + gap + "）");
            h.succeed();
        } finally {
            cleanup(h, v, p);
        }
    }

    /** TC-T19-143：生成村庄想建的建筑没有预留地块，临时找地划出地块并开工。 */
    @GameTest(template = T, batch = "foundation_adhoc", timeoutTicks = 200)
    public static void generated_village_adhoc_plot(GameTestHelper h) {
        VillageWorkGameTests.TV tv = VillageMigrationGameTests.village(h, 0, Age.CLASSICAL, java.util.Map.of(), java.util.List.of());
        try {
            VillageEconomy.State s = VillageEconomy.get(h.getLevel().getServer()).state(tv.id());
            var ctx = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
            h.assertTrue(ctx.plan().info().plots().isEmpty(), "没有预留地块");
            Optional<BuildProject> pr = Construction.daily(ctx);
            h.assertTrue(pr.isPresent() && pr.get().plot.startsWith(AdHocSites.PREFIX), "临时地块开工：" + pr.map(x -> x.building));
            h.assertTrue(VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id()).orElseThrow().info().plots().stream()
                    .anyMatch(x -> x.id().equals(pr.get().plot)), "地块追加进规划数据");
            h.assertTrue(s.activeProject().isPresent(), "进入备料");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }
}
