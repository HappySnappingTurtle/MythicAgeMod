package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.GreekPrayer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentDialogue;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentTrade;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentTradeView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * T19 阶段 ④b：居民交易（TC-T19-107～110、116～118 的世界侧）与对话优先级（TC-111）。希腊真实数据（职业交易表）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageTradeGameTests {
    private static final String BATCH = "village_trade";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private VillageTradeGameTests() {
    }

    static FakePlayer player(GameTestHelper h, int emeralds) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "trader"));
        p.getInventory().clearContent();
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 10))));
        if (emeralds > 0) {
            p.getInventory().add(new ItemStack(Items.EMERALD, emeralds));
        }
        OnlinePlayers.forTest(p, true);
        return p;
    }

    static int count(FakePlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static ResidentTradeView.Row row(FakePlayer p, ResidentEntity r, String id) {
        return ResidentTrade.view(p, r).orElseThrow().rows().stream().filter(x -> x.id().equals(id)).findFirst()
                .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("没有这条交易：" + id));
    }

    /** TC-T19-107（验收：缺货不能买）+ 117 + 110：从村库出货；没货显示缺货；口粮卖到底线；居民界面里没有祭司的恩宠商品。 */
    @GameTest(template = T, batch = BATCH)
    public static void buy_from_village_stock(GameTestHelper h) {
        VillageWorkGameTests.TV tv = VillageMigrationGameTests.village(h, 2, Age.ARCHAIC, Map.of(), List.of());
        FakePlayer p = player(h, 20);
        try {
            ResidentEntity farmer = VillageMigrationGameTests.settler(h, tv, "greek:farmer", 0);
            VillageEconomy.State s = VillageMigrationGameTests.state(h, tv);
            s.stock.take("mythic_age:olive_oil", 1000, 0);
            h.assertTrue(row(p, farmer, "olive_oil").state() == ResidentTradeView.State.OUT_OF_STOCK, "没有橄榄油：缺货");
            h.assertTrue(ResidentTrade.deal(p, farmer, "olive_oil") == ResidentTradeView.State.OUT_OF_STOCK, "不能买");
            s.stock.putUnchecked("mythic_age:olive_oil", 2);
            h.assertTrue(ResidentTrade.deal(p, farmer, "olive_oil") == ResidentTradeView.State.OK, "有货能买");
            h.assertTrue(count(p, BuiltInRegistries.ITEM.get(ResourceLocation.parse("mythic_age:olive_oil"))) == 1
                    && count(p, Items.EMERALD) == 19 && s.stock.count("mythic_age:olive_oil") == 1, "村库出 1、付 1 绿宝石");
            // 口粮：薄饼 = 口粮总量；底线 24（希腊数据）
            s.stock.take("mythic_age:olive_flatbread", 1000, 0);
            s.stock.putUnchecked("mythic_age:olive_flatbread", 30);
            h.assertTrue(ResidentTrade.deal(p, farmer, "flatbread") == ResidentTradeView.State.OK, "卖 1 份（6 个）");
            h.assertTrue(row(p, farmer, "flatbread").state() == ResidentTradeView.State.OUT_OF_STOCK, "第 2 份会跌破口粮底线");
            h.assertTrue(ResidentTrade.view(p, farmer).orElseThrow().rows().stream().noneMatch(r -> r.id().startsWith("gp_")),
                    "D8：祭司的恩宠兑换不在居民这里");
            h.assertTrue(ResidentTrade.view(p, farmer).orElseThrow().rows().stream().noneMatch(r -> r.id().equals("athena_oil")),
                    "远古、没有雅典娜祠：次神商品不出现");
            h.succeed();
        } finally {
            OnlinePlayers.forTest(p, false);
            VillageMigrationGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-108：收购满仓拒收，玩家的东西不动；有空就收进村库、付绿宝石。 */
    @GameTest(template = T, batch = BATCH)
    public static void sell_refused_when_full(GameTestHelper h) {
        VillageWorkGameTests.TV tv = VillageMigrationGameTests.village(h, 2, Age.ARCHAIC, Map.of(), List.of());
        FakePlayer p = player(h, 0);
        try {
            ResidentEntity potter = VillageMigrationGameTests.settler(h, tv, "greek:potter", 0);
            VillageEconomy.State s = VillageMigrationGameTests.state(h, tv);
            p.getInventory().add(new ItemStack(Items.CLAY_BALL, 16));
            s.stock.setCapacity(s.stock.total() + 4);
            h.assertTrue(ResidentTrade.deal(p, potter, "clay") == ResidentTradeView.State.FULL, "装不下拒收");
            h.assertTrue(count(p, Items.CLAY_BALL) == 16 && count(p, Items.EMERALD) == 0, "玩家的东西不动");
            s.stock.setCapacity(s.stock.total() + 100);
            int before = s.stock.count("minecraft:clay_ball");
            h.assertTrue(ResidentTrade.deal(p, potter, "clay") == ResidentTradeView.State.OK, "有空就收");
            h.assertTrue(count(p, Items.CLAY_BALL) == 0 && count(p, Items.EMERALD) == 1
                    && s.stock.count("minecraft:clay_ball") == before + 16, "收进村库、付绿宝石");
            h.assertTrue(ResidentTrade.deal(p, potter, "clay") == ResidentTradeView.State.NO_ITEM, "没东西可卖");
            h.succeed();
        } finally {
            OnlinePlayers.forTest(p, false);
            VillageMigrationGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-109（F7）+ 116 + 118：当天祈祷过的居民额度减半；次神商品与折扣；杂货不扣村库。 */
    @GameTest(template = T, batch = BATCH)
    public static void daily_limits_gods_and_imports(GameTestHelper h) {
        VillageWorkGameTests.TV tv = VillageMigrationGameTests.village(h, 3, Age.CLASSICAL, Map.of(), List.of());
        FakePlayer p = player(h, 64);
        try {
            ResidentEntity farmer = VillageMigrationGameTests.settler(h, tv, "greek:farmer", 0);
            VillageEconomy.State s = VillageMigrationGameTests.state(h, tv);
            s.stock.putUnchecked("mythic_age:olive", 500);
            h.assertTrue(row(p, farmer, "olive").remaining() == 8, "平时 8 份");
            long today = MythDays.today(h.getLevel().getServer());
            farmer.getData(GreekPrayer.RECORD).record(today, UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), BlockPos.ZERO));
            h.assertTrue(row(p, farmer, "olive").remaining() == 4, "F7：当天祈祷过减半");
            for (int i = 0; i < 4; i++) {
                ResidentTrade.deal(p, farmer, "olive");
            }
            h.assertTrue(ResidentTrade.deal(p, farmer, "olive") == ResidentTradeView.State.DAILY_DONE, "额度用完");
            // 次神商品：本村立过雅典娜祠才出现；供奉者九折
            boolean before = ResidentTrade.view(p, farmer).orElseThrow().rows().stream().anyMatch(r -> r.id().equals("athena_oil"));
            h.assertTrue(!before, "没有雅典娜祠：不出现");
            VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
            reg.update(reg.byId(tv.id()).orElseThrow().withRite(Age.CLASSICAL, Age.CLASSICAL, "athena"));
            h.assertTrue(row(p, farmer, "athena_oil").price() == 2, "不是供奉者：原价 2");
            MythData.get(p).civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "athena");
            h.assertTrue(row(p, farmer, "athena_oil").price() == 1 && row(p, farmer, "athena_oil").basePrice() == 2,
                    "供奉者打折：至少便宜 1");
            MythData.get(p).civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "hermes");
            // 杂货：集市商人（古典开放）
            ResidentEntity merchant = VillageMigrationGameTests.settler(h, tv, "greek:merchant", 1);
            int total = s.stock.total();
            h.assertTrue(row(p, merchant, "map").stock() == -1, "杂货不看村库");
            h.assertTrue(row(p, merchant, "map").price() == 2, "赫尔墨斯供奉者全场打折：3 → 2");
            h.assertTrue(ResidentTrade.deal(p, merchant, "map") == ResidentTradeView.State.OK && s.stock.total() == total,
                    "买杂货不扣村库");
            h.assertTrue(count(p, Items.MAP) == 1, "拿到地图");
            h.succeed();
        } finally {
            OnlinePlayers.forTest(p, false);
            VillageMigrationGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-111：对话优先级——警戒 > 本职缺料 > 在建项目 > 闲话（有活时说正在做的事）。 */
    @GameTest(template = T, batch = BATCH)
    public static void dialogue_priority(GameTestHelper h) {
        VillageWorkGameTests.TV tv = VillageMigrationGameTests.village(h, 2, Age.ARCHAIC, Map.of(), List.of());
        try {
            ResidentEntity potter = VillageMigrationGameTests.settler(h, tv, "greek:potter", 0);
            VillageEconomy.State s = VillageMigrationGameTests.state(h, tv);
            var server = h.getLevel().getServer();
            s.stock.putUnchecked("minecraft:clay_ball", 10);
            h.assertTrue(ResidentDialogue.kind(server, potter) == ResidentDialogue.Kind.CHATTER, "平时闲话");
            s.stock.take("minecraft:clay_ball", 1000, 0);
            h.assertTrue(ResidentDialogue.kind(server, potter) == ResidentDialogue.Kind.SHORT, "没黏土：本职缺料");
            h.assertTrue(ResidentDialogue.line(server, potter).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                    && tc.getKey().equals("mythic_age.village.say.greek.potter.short"), "陶工的缺料句");
            io.github.happysnappingturtle.mythicage.village.work.VillageScheduler.runtime(tv.id());
            alarm(tv.id(), WorldClock.now(server) + 100);
            h.assertTrue(ResidentDialogue.kind(server, potter) == ResidentDialogue.Kind.ALARM, "警戒优先");
            h.succeed();
        } finally {
            VillageMigrationGameTests.cleanup(h, tv);
        }
    }

    static void alarm(UUID village, long until) {
        io.github.happysnappingturtle.mythicage.village.work.VillageScheduler.alarmForTest(village, until);
    }
}
