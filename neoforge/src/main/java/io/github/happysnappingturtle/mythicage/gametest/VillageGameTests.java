package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageResolver;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** T10a 聚落归属与主神（TC-T10a-40～55）。原版村庄结构判定用 {@link VillageResolver#overrideTypeForTest} 注入。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageGameTests {
    private static final String BATCH = "village";
    private static final String T = GameTestTemplates.EMPTY_11X11;

    private VillageGameTests() {
    }

    private static BlockPos bell(GameTestHelper h, int x, int z) {
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 11; j++) {
                h.setBlock(i, 0, j, Blocks.STONE);
            }
        }
        BlockPos rel = new BlockPos(x, 1, z);
        h.setBlock(rel, Blocks.BELL);
        return h.absolutePos(rel);
    }

    private static Optional<VillageRegistry.Village> registerAs(GameTestHelper h, BlockPos bell, String type) {
        VillageResolver.overrideTypeForTest((l, b) -> b.equals(bell) ? Optional.ofNullable(type) : Optional.empty());
        try {
            return VillageService.register(h.getLevel(), bell);
        } finally {
            VillageResolver.overrideTypeForTest(null);
        }
    }

    private static GlobalPos g(GameTestHelper h, BlockPos p) {
        return GlobalPos.of(h.getLevel().dimension(), p);
    }

    // ───────────── 登记（TC-T10a-40～44） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void plains_village_is_greek(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        VillageRegistry.Village v = registerAs(h, bell, "plains").orElseThrow();
        h.assertTrue(v.civ().equals("greek"), "平原村庄应为希腊，实际 " + v.civ());
        h.assertTrue(Set.of("zeus", "poseidon", "hades").contains(v.patron()), "主神应为三主神之一，实际 " + v.patron());
        h.assertTrue(VillageRegistry.get(h.getLevel().getServer()).byBell(g(h, bell)).isPresent(), "已登记");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void desert_village_is_egyptian_without_priest(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        VillageRegistry.Village v = registerAs(h, bell, "desert").orElseThrow();
        h.assertTrue(v.civ().equals("egyptian") && v.patron().isEmpty(), "沙漠村庄为埃及，尚无主神表：" + v);
        VillageResolver.overrideTypeForTest((l, b) -> b.equals(bell) ? Optional.of("desert") : Optional.empty());
        try {
            GuardianSpawner.trySpawnNear(h.getLevel(), bell);
        } finally {
            VillageResolver.overrideTypeForTest(null);
        }
        h.assertTrue(h.getLevel().getEntitiesOfClass(GuardianEntity.class,
                new net.minecraft.world.phys.AABB(bell).inflate(5)).isEmpty(), "埃及村庄不生成希腊祭司");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void player_bell_is_not_a_village_and_cached(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        AtomicInteger calls = new AtomicInteger();
        VillageResolver.overrideTypeForTest((l, b) -> {
            calls.incrementAndGet();
            return Optional.empty();
        });
        try {
            h.assertTrue(VillageService.register(h.getLevel(), bell).isEmpty(), "不在村庄结构里的钟不登记");
            VillageService.register(h.getLevel(), bell);
        } finally {
            VillageResolver.overrideTypeForTest(null);
        }
        h.assertTrue(calls.get() == 1, "第二次走缓存，实际判定 " + calls.get() + " 次");
        h.assertTrue(VillageRegistry.get(h.getLevel().getServer()).byBell(g(h, bell)).isEmpty(), "注册表里没有");
        h.succeed();
    }

    @GameTest(template = T, batch = "village_config")
    public static void neutral_config_and_reset(GameTestHelper h) {
        BlockPos bellA = bell(h, 3, 3);
        h.setBlock(new BlockPos(8, 1, 8), Blocks.BELL);
        BlockPos bellB = h.absolutePos(new BlockPos(8, 1, 8));
        ConfigHolder<VillagesConfig> holder = ModConfigs.villages();
        VillagesConfig original = holder.current();
        VillageRegistry.Village a = registerAs(h, bellA, "plains").orElseThrow();
        try {
            holder.reload("{\"schemaVersion\":1,\"vanillaVillages\":\"neutral\"}");
            VillageRegistry.Village b = registerAs(h, bellB, "plains").orElseThrow();
            h.assertTrue(b.neutral() && b.patron().isEmpty(), "中立配置：新登记的村庄无信仰");
            h.assertTrue(VillageRegistry.get(h.getLevel().getServer()).byBell(g(h, bellA)).orElseThrow().equals(a),
                    "已登记的村庄不受配置改动影响");
        } finally {
            holder.reload(io.github.happysnappingturtle.mythicage.core.config.ConfigLoader.toPrettyJson(holder.schema(), original));
        }
        VillageRegistry.get(h.getLevel().getServer()).remove(g(h, bellB));
        VillageService.forget(g(h, bellB));
        VillageRegistry.Village again = registerAs(h, bellB, "plains").orElseThrow();
        h.assertTrue(again.civ().equals("greek"), "reset 后按当前配置重新登记");
        h.succeed();
    }

    // ───────────── 成员（TC-T10a-45～46） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void villager_membership_follows_meeting_point(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        VillageRegistry.Village v = registerAs(h, bell, "plains").orElseThrow();
        Villager member = h.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        member.getBrain().setMemory(MemoryModuleType.MEETING_POINT, g(h, bell));
        Villager loner = h.spawn(EntityType.VILLAGER, new BlockPos(8, 1, 8));
        loner.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);
        h.assertTrue(VillageResolver.villageOf(member).map(VillageRegistry.Village::id).equals(Optional.of(v.id())),
                "聚会点指向该钟 → 该村成员");
        h.assertTrue(VillageResolver.villageOf(loner).isEmpty(), "没有聚会点 → 零散村民");
        loner.getBrain().setMemory(MemoryModuleType.MEETING_POINT, g(h, bell.offset(30, 0, 0)));
        h.assertTrue(VillageResolver.villageOf(loner).isEmpty(), "指向未登记的钟 → 零散村民");
        h.succeed();
    }

    // ───────────── 祭司与结识主神（TC-T10a-47～49） ─────────────

    private static VillageRegistry.Village village(GameTestHelper h, BlockPos bell, String patron) {
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), g(h, bell), "greek", patron,
                VillageRegistry.Origin.VANILLA, null, "plains", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        return v;
    }

    private static FakePlayer player(GameTestHelper h, GuardianEntity g, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(g.getX() + 2, g.getY(), g.getZ());
        MythData.get(p).civ(Civilization.GREEK).setAge(Age.CLASSICAL);
        return p;
    }

    private static GuardianEntity priest(GameTestHelper h, BlockPos bell) {
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), bell.offset(2, 0, 0), Optional.of(bell), Civilization.GREEK);
        if (g == null) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("祭司没有生成");
        }
        g.setNoAi(true);
        return g;
    }

    @GameTest(template = T, batch = BATCH)
    public static void talking_to_village_priest_learns_patron(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        village(h, bell, "hades");
        GuardianEntity g = priest(h, bell);
        h.assertTrue(GuardianSpawner.villageOf(g).map(VillageRegistry.Village::patron).equals(Optional.of("hades")),
                "祭司属于该村庄");
        FakePlayer p = player(h, g, "ma_learn");
        TradeService.openGuardian(p, g);
        TradeService.openGuardian(p, g);
        PlayerMythData d = MythData.get(p);
        h.assertTrue(d.civ(Civilization.GREEK).knownPatrons().equals(Set.of("hades")), "结识哈迪斯：" + d.civ(Civilization.GREEK).knownPatrons());
        h.assertTrue(d.receipts().contains(io.github.happysnappingturtle.mythicage.core.player.ReceiptBook.Scope.UNIQUE,
                "known_patron:greek:hades"), "首次结识写入唯一收据");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void egg_priest_teaches_nothing(GameTestHelper h) {
        bell(h, 5, 5);
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 3)), Optional.empty(), Civilization.GREEK);
        h.assertTrue(g != null, "生成");
        FakePlayer p = player(h, g, "ma_egg");
        TradeService.openGuardian(p, g);
        h.assertTrue(MythData.get(p).civ(Civilization.GREEK).knownPatrons().isEmpty(), "不属于村庄的祭司不提供结识");
        h.succeed();
    }

    // ───────────── 交易表的次神、主神字段（TC-T10a-50～52） ─────────────

    private static final String TABLE = """
            {"schemaVersion":1,"civilization":"greek","offers":[
             {"id":"bolt","category":"god_power","result":{"item":"minecraft:paper","count":1},"price":{"favor":20},
              "minAge":"archaic","venues":["guardian"],"patron":"zeus","patronDiscount":0.1},
             {"id":"aegis","category":"supplies","result":{"item":"minecraft:shield","count":1},"price":{"favor":20},
              "minAge":"classical","venues":["guardian"],"minorGod":"athena","minorGodDiscount":{"athena":0.1}}]}""";

    @GameTest(template = T, batch = "village_trade")
    public static void minor_god_and_patron_fields(GameTestHelper h) {
        TradeTableLoader.overrideForTest(TradeTable.parse(TABLE, TradeTableLoader.ITEMS));
        TradeService.setDiscountProvider((pl, o) -> o.id().equals("aegis") ? java.util.List.of(0.1) : java.util.List.of());
        try {
            BlockPos zeusBell = bell(h, 2, 2);
            VillageRegistry.Village zeus = village(h, zeusBell, "zeus");
            h.setBlock(new BlockPos(9, 1, 9), Blocks.BELL);
            BlockPos hadesBell = h.absolutePos(new BlockPos(9, 1, 9));
            village(h, hadesBell, "hades");
            GuardianEntity zeusPriest = priest(h, zeusBell);
            GuardianEntity hadesPriest = priest(h, hadesBell);
            FakePlayer p = player(h, zeusPriest, "ma_gods");
            MythData.get(p).civ(Civilization.GREEK).setFavor(100);
            TradeService.openGuardian(p, zeusPriest);
            // T11a：次神商品只在立了该次神祠的村庄出售（对所有人），供奉者再打折
            var o1 = TradeService.buy(p, "aegis", 18, UUID.randomUUID().toString());
            h.assertTrue(o1.reason() == TradeReason.NO_SHRINE, "宙斯村没有雅典娜祠：" + o1.reason());
            VillageRegistry.get(h.getLevel().getServer()).update(zeus.withRite(Age.CLASSICAL, Age.CLASSICAL, "athena"));
            var o1b = TradeService.buy(p, "aegis", 18, UUID.randomUUID().toString());
            h.assertTrue(o1b.reason() == TradeReason.OK, "立祠后未供奉者也能买，20×0.9=18：" + o1b.reason() + " " + o1b.arg());
            MythData.get(p).civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "athena");
            var o2 = TradeService.buy(p, "aegis", 17, UUID.randomUUID().toString());
            h.assertTrue(o2.reason() == TradeReason.OK, "供奉后 20×0.9×0.9=16.2 → 17：" + o2.reason() + " " + o2.arg());
            var o3 = TradeService.buy(p, "bolt", 18, UUID.randomUUID().toString());
            h.assertTrue(o3.reason() == TradeReason.OK, "宙斯村雷击九折 18：" + o3.reason() + " " + o3.arg());
            p.moveTo(hadesPriest.getX() + 2, hadesPriest.getY(), hadesPriest.getZ());
            TradeService.openGuardian(p, hadesPriest);
            var o4 = TradeService.buy(p, "bolt", 20, UUID.randomUUID().toString());
            h.assertTrue(o4.reason() == TradeReason.WRONG_PATRON, "T11a：哈迪斯村不卖宙斯的远古神力：" + o4.reason());
        } finally {
            TradeTableLoader.overrideForTest(null);
            TradeService.setDiscountProvider((pl, o) -> java.util.List.of());
        }
        h.succeed();
    }

    // ───────────── 钟被拆、进村标题、存档（TC-T10a-53～55） ─────────────

    @GameTest(template = T, batch = "village_expire")
    public static void missing_bell_expires_after_seven_days(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        registerAs(h, bell, "plains").orElseThrow();
        h.setBlock(new BlockPos(5, 1, 5), Blocks.AIR);
        h.runAfterDelay(2, () -> {
            VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
            VillageService.checkBells(h.getLevel(), bell);
            h.assertTrue(reg.byBell(g(h, bell)).orElseThrow().missingSinceDay() >= 0, "记下钟不在的起始日");
            MythDays.advanceForTest(6);
            try {
                VillageService.checkBells(h.getLevel(), bell);
                h.assertTrue(reg.byBell(g(h, bell)).isPresent(), "第 6 日仍保留");
                MythDays.advanceForTest(1);
                VillageService.checkBells(h.getLevel(), bell);
                h.assertTrue(reg.byBell(g(h, bell)).isEmpty(), "第 7 日注销");
            } finally {
                MythDays.advanceForTest(-7);
            }
            h.succeed();
        });
    }

    /** 离本村钟 ≥100 格、且 128 格内没有任何已登记村庄的位置（按距离由近到远找）。 */
    private static BlockPos farFromVillages(GameTestHelper h, BlockPos bell) {
        var all = VillageRegistry.get(h.getLevel().getServer()).all();
        for (int d = 100; d <= 2000; d += 100) {
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                BlockPos c = bell.offset(dir[0] * d, 0, dir[1] * d);
                boolean clear = all.stream().noneMatch(v -> v.bell().dimension() == h.getLevel().dimension()
                        && v.bell().pos().distToCenterSqr(c.getX(), v.bell().pos().getY(), c.getZ()) < 128 * 128);
                if (clear) {
                    return c;
                }
            }
        }
        throw new IllegalStateException("找不到远离村庄的位置");
    }

    @GameTest(template = GameTestTemplates.EMPTY_17X16X36, batch = "village_title")
    public static void title_shows_on_entering_only(GameTestHelper h) {
        BlockPos rel = new BlockPos(8, 1, 2);
        h.setBlock(rel, Blocks.BELL);
        BlockPos bell = h.absolutePos(rel);
        village(h, bell, "zeus");
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_title"));
        // 「村外」要真的没有村庄：并行的其它测试结构里也登记了村庄，固定偏移可能正好落进别人的村子
        BlockPos outside = farFromVillages(h, bell);
        p.moveTo(outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5);
        h.assertTrue(VillageService.titleFor(p).isEmpty(), "村外不显示");
        p.moveTo(bell.getX() + 10.5, bell.getY(), bell.getZ());
        h.assertTrue(VillageService.titleFor(p).isPresent(), "进入时显示");
        p.moveTo(bell.getX() + 20.5, bell.getY(), bell.getZ());
        h.assertTrue(VillageService.titleFor(p).isEmpty(), "村内移动不重复");
        p.moveTo(outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5);
        VillageService.titleFor(p);
        p.moveTo(bell.getX() + 10.5, bell.getY(), bell.getZ());
        h.assertTrue(VillageService.titleFor(p).isPresent(), "离开 64 格后再进入，再次显示");
        h.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void registry_and_player_round_trip(GameTestHelper h) {
        VillageRegistry reg = new VillageRegistry();
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), g(h, new BlockPos(1, 2, 3)), "greek", "poseidon",
                VillageRegistry.Origin.VANILLA, null, "plains", 4);
        reg.add(v);
        CompoundTag tag = reg.save(new CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(VillageRegistry.read(tag, h.getLevel().registryAccess()).byBell(v.bell()).equals(Optional.of(v)),
                "村庄登记往返一致");
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).knownPatrons().add("zeus");
        d.civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "hermes");
        h.assertTrue(io.github.happysnappingturtle.mythicage.player.PlayerDataNbt.read(
                io.github.happysnappingturtle.mythicage.player.PlayerDataNbt.write(d)).equals(d), "玩家已结识主神与次神往返一致");
        h.succeed();
    }
}
