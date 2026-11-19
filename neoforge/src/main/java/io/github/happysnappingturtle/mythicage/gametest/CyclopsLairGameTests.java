package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.GreekCyclops;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.lair.LairRules;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.entity.cyclops.ShepherdCyclopsEntity;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.lair.EliteLairs;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.structure.LairStructure;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import io.github.happysnappingturtle.mythicage.structure.StructureSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T25a 阶段 ⑤：独眼巨人洞穴与精英巢穴（TC-T25a-71 封洞部分、72、73、78、95、96）。
 * 洞穴摆在远处的槽位（x = 48000 + n·96），先用石头堆一座山（洞口朝南 +Z、洞口外是平地），再按 R0 摆模板、登记。
 * 夜里的时刻整批固定在 13000（{@link EliteLairs#dayTimeForTest}），不动全服时间；白天、天亮的切换在 core 的 LairSiteRulesTest 测。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyclopsLairGameTests {
    private static final String BATCH = "cyclops_lair";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final AtomicInteger SLOT = new AtomicInteger();
    private static final int FLOOR = 120;
    private static final long NIGHT = 13000;

    private CyclopsLairGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        EliteLairs.dayTimeForTest(NIGHT);
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        EliteLairs.dayTimeForTest(null);
    }

    record Cave(StructureStart start, StructureInfoPiece.Info info, String key, long placeNanos, int chunks) {
        BlockPos one(String anchor) {
            return info.anchors(anchor).get(0);
        }
    }

    static Structure structure(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "cyclops_cave")));
    }

    /** 在新槽位堆山、摆一座洞穴并登记（生成牧人与羊）。 */
    static Cave place(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int minX = 48000 + SLOT.getAndIncrement() * 96;
        int minZ = 48000;
        for (int cx = (minX - 16) >> 4; cx <= (minX + 40) >> 4; cx++) {
            for (int cz = (minZ - 16) >> 4; cz <= (minZ + 56) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        // 清场，再堆山：洞身那一段（z < 23）从地面下 3 格堆到地面上 12 格；洞口外（z 23..40）是平地
        for (BlockPos p : BlockPos.betweenClosed(minX - 8, FLOOR - 4, minZ - 8, minX + 24, FLOOR + 18, minZ + 40)) {
            int z = p.getZ() - minZ;
            int y = p.getY() - FLOOR;
            boolean hill = z < 23 && y < 12;
            boolean ground = y < 0;
            level.setBlock(p, hill || ground ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
        StructureStart start = LairStructure.startForTest(level, structure(level), Turn.R0, minX, FLOOR, minZ);
        BoundingBox bb = start.getBoundingBox();
        ChunkPos a = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos b = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        int[] n = {0};
        long t0 = System.nanoTime();
        ChunkPos.rangeClosed(a, b).forEach(c -> {
            n[0]++;
            start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(),
                            level.getMaxBuildHeight(), c.getMaxBlockZ()), c);
        });
        long nanos = System.nanoTime() - t0;
        StructureInfoPiece.Info info = StructureRegistration.info(start).orElseThrow(() -> new GameTestAssertException("缺元数据件"));
        h.assertTrue(StructureRegistration.register(level, start) == StructureRegistration.Result.REGISTERED, "应登记");
        return new Cave(start, info, GreekCyclops.lairKey(level, info.anchors("cyclops").get(0)), nanos, n[0]);
    }

    static List<ShepherdCyclopsEntity> shepherds(ServerLevel level, Cave c) {
        return level.getEntitiesOfClass(ShepherdCyclopsEntity.class, new AABB(c.one("cyclops")).inflate(64),
                e -> e.isAlive() && c.key().equals(e.lairKey()));
    }

    static List<Sheep> sheep(ServerLevel level, Cave c) {
        return level.getEntitiesOfClass(Sheep.class, new AABB(c.one("cyclops")).inflate(64),
                e -> e.isAlive() && c.key().equals(e.getPersistentData().getString(EliteLairs.LAIR_KEY)));
    }

    /** 牧人放到睡处、关掉 AI（封洞条件是他在洞里）。 */
    static ShepherdCyclopsEntity asleep(ServerLevel level, Cave c) {
        ShepherdCyclopsEntity s = shepherds(level, c).get(0);
        BlockPos sl = c.one("sleep");
        s.setNoAi(true);
        s.teleportTo(sl.getX() + 0.5, sl.getY(), sl.getZ() + 0.5);
        return s;
    }

    static int boulders(ServerLevel level, Cave c) {
        int n = 0;
        for (BlockPos p : c.info().anchors("entrance")) {
            if (level.getBlockState(p).is(GreekCyclops.CAVE_BOULDER.get())) {
                n++;
            }
        }
        return n;
    }

    /** 跨很多刻的用例要强制加载洞穴所在区块：远处槽位没人，区块一卸载临时方块就会复原。 */
    static void force(ServerLevel level, Cave c, boolean on) {
        BoundingBox bb = c.start().getBoundingBox();
        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                level.setChunkForced(cx, cz, on);
            }
        }
    }

    static void cleanup(ServerLevel level, Cave c) {
        shepherds(level, c).forEach(Mob::discard);
        sheep(level, c).forEach(Mob::discard);
        for (BlockPos p : c.info().anchors("entrance")) {
            TempBlocks.restoreNow(level, p);
        }
        EliteLairs.forgetForTest(level.getServer(), c.key());
        StructureSites.get(level.getServer()).forgetForTest(StructureRegistration.key(level, c.start()));
    }

    /** TC-T25a-95、96：放置耗时；洞内净空（中轴 ≥ 5 格高）、登记生成 1 位牧人与 6 只羊、宝库设表。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cave_places_and_registers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        try {
            double msPerChunk = c.placeNanos() / 1.0e6 / Math.max(1, c.chunks());
            h.assertTrue(msPerChunk < 50, "单区块放置 < 50 ms，实际 " + msPerChunk);
            BlockPos home = c.one("cyclops");
            BlockPos sleep = c.one("sleep");
            BlockPos wine = c.one("wine_area");
            for (BlockPos base : List.of(home, sleep, wine)) {
                for (int dy = 1; dy <= 4; dy++) {
                    h.assertTrue(level.getBlockState(base.above(dy)).isAir(), "中轴 " + base + " 上方 " + dy + " 格应是空的");
                }
                h.assertTrue(level.getBlockState(base.below()).isSolid(), "脚下是地面：" + base);
            }
            for (BlockPos p : c.info().anchors("entrance")) {
                h.assertTrue(level.getBlockState(p).isAir(), "洞口白天是通的：" + p);
            }
            h.assertTrue(shepherds(level, c).size() == 1, "1 位牧人，实际 " + shepherds(level, c).size());
            ShepherdCyclopsEntity s = shepherds(level, c).get(0);
            h.assertTrue(Math.abs(s.getMaxHealth() - 160) < 0.01 && Math.abs(s.getHealth() - 160) < 0.01, "牧人生命 160");
            h.assertTrue(sheep(level, c).size() == GreekCyclops.FLOCK, "6 只羊，实际 " + sheep(level, c).size());
            BlockPos v = c.one("vault");
            h.assertTrue(level.getBlockEntity(v) instanceof RuinVaultBlockEntity, "宝库方块在");
        } finally {
            cleanup(level, c);
        }
        h.succeed();
    }

    /** 用户 11-18：洞里不能太暗——等光照算完，洞内各锚点处的方块光照 ≥ 9（火堆 + 洞顶隐形光源）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void cave_is_lit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        force(level, c, true);
        h.runAfterDelay(60, () -> {
            try {
                for (String a : List.of("sleep", "cyclops", "wine_area", "pen", "vault")) {
                    for (BlockPos p : c.info().anchors(a)) {
                        BlockPos at = level.getBlockState(p).isAir() ? p : p.above();
                        int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at);
                        h.assertTrue(light >= 9, a + " 处方块光照应 ≥ 9，实际 " + light);
                    }
                }
            } finally {
                cleanup(level, c);
                force(level, c, false);
            }
            h.succeed();
        });
    }

    /** TC-T25a-71（封洞部分）：夜里牧人在洞里 → 洞口 3 × 5 被封洞巨石封住，到期时刻是天亮；牧人不在洞里就不封。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void seals_at_night_when_inside(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        try {
            ShepherdCyclopsEntity s = shepherds(level, c).get(0);
            s.setNoAi(true);
            BlockPos out = c.one("flock");
            s.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
            EliteLairs.sealForTest(level, c.key());
            h.assertTrue(boulders(level, c) == 0, "牧人在洞外时不封");
            asleep(level, c);
            EliteLairs.sealForTest(level, c.key());
            int n = c.info().anchors("entrance").size();
            h.assertTrue(boulders(level, c) == n, "应封住洞口 " + n + " 格，实际 " + boulders(level, c));
            long now = WorldClock.now(level.getServer());
            long until = TempBlocks.get(level.getServer()).entries().stream()
                    .filter(e -> e.pos().pos().equals(c.one("entrance"))).findFirst().orElseThrow().until();
            h.assertTrue(Math.abs(until - now - (LairRules.SEAL_UNTIL - NIGHT)) <= 1, "到天亮（还剩 10000 刻）移开，实际 " + (until - now));
        } finally {
            cleanup(level, c);
        }
        h.succeed();
    }

    /** TC-T25a-73：巨石复原了（区块卸载时 TempBlocks 会复原）→ 下一次检查当晚重新封上。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void reseals_after_restore(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        try {
            asleep(level, c);
            EliteLairs.sealForTest(level, c.key());
            for (BlockPos p : c.info().anchors("entrance")) {
                TempBlocks.restoreNow(level, p);
            }
            h.assertTrue(boulders(level, c) == 0, "模拟卸载：巨石复原、不残留");
            EliteLairs.sealForTest(level, c.key());
            h.assertTrue(boulders(level, c) == c.info().anchors("entrance").size(), "当晚重新封上");
        } finally {
            cleanup(level, c);
        }
        h.succeed();
    }

    /** TC-T25a-72（D15）：洞内贴着巨石潜行右键 3 秒 → 那一列最下面两格让开，当晚不再封；洞外右键推不动。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void push_boulder_from_inside(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        force(level, c, true);
        asleep(level, c);
        EliteLairs.sealForTest(level, c.key());
        BlockPos low = c.info().anchors("entrance").stream().filter(p -> p.getY() == c.one("cyclops").getY())
                .filter(p -> p.getX() == c.one("cyclops").getX()).findFirst().orElseThrow();
        FakePlayer outside = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ma_lair_out"));
        FakePlayer inside = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ma_lair_in"));
        Vec3 out = Vec3.atBottomCenterOf(low.south(2));
        Vec3 in = Vec3.atBottomCenterOf(low.north(1));
        outside.moveTo(out.x, out.y, out.z);
        inside.moveTo(in.x, in.y, in.z);
        outside.setShiftKeyDown(true);
        inside.setShiftKeyDown(true);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(low), Direction.NORTH, low, false);
        for (int i = 0; i <= 20; i++) {
            int tick = i * 4;
            h.runAfterDelay(tick + 1, () -> {
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(outside, InteractionHand.MAIN_HAND, low, hit));
            });
        }
        h.runAfterDelay(90, () -> {
            h.assertTrue(level.getBlockState(low).is(GreekCyclops.CAVE_BOULDER.get()), "洞外推不动");
            for (int i = 0; i <= 18; i++) {
                h.runAfterDelay(i * 4 + 1, () ->
                        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(inside, InteractionHand.MAIN_HAND, low, hit)));
            }
        });
        h.runAfterDelay(170, () -> {
            try {
                h.assertTrue(level.getBlockState(low).isAir() && level.getBlockState(low.above()).isAir(), "最下面两格让开");
                h.assertTrue(level.getBlockState(low.above(2)).is(GreekCyclops.CAVE_BOULDER.get()), "上面的还在");
                EliteLairs.sealForTest(level, c.key());
                h.assertTrue(level.getBlockState(low).isAir(), "当晚不再封");
            } finally {
                cleanup(level, c);
                force(level, c, false);
            }
            h.succeed();
        });
    }

    /** TC-T25a-78：牧人死后清空；7 日后满血牧人回来、羊补足到 6；宝库不重置。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void repopulates_after_seven_days(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Cave c = place(h);
        try {
            ShepherdCyclopsEntity old = shepherds(level, c).get(0);
            old.kill();
            EliteLairs.Lair lair = EliteLairs.get(level.getServer()).lair(c.key());
            long today = MythDays.today(level.getServer());
            h.assertTrue(lair != null && lair.clearedDay == today, "死后记下清空日，实际 " + (lair == null ? null : lair.clearedDay));
            List<Sheep> flock = sheep(level, c);
            for (int i = 0; i < 4; i++) {
                flock.get(i).discard();
            }
            EliteLairs.setClearedForTest(level.getServer(), c.key(), today - 6);
            EliteLairs.tick(level.getServer());
            h.assertTrue(shepherds(level, c).isEmpty(), "第 6 天不入住");
            EliteLairs.setClearedForTest(level.getServer(), c.key(), today - LairRules.VACANT_DAYS);
            EliteLairs.tick(level.getServer());
            List<ShepherdCyclopsEntity> now = shepherds(level, c);
            h.assertTrue(now.size() == 1 && now.get(0) != old, "第 7 天新牧人入住");
            h.assertTrue(Math.abs(now.get(0).getHealth() - now.get(0).getMaxHealth()) < 0.01, "满血");
            h.assertTrue(sheep(level, c).size() == GreekCyclops.FLOCK, "羊补足到 6，实际 " + sheep(level, c).size());
            h.assertTrue(lair.clearedDay == LairRules.NOT_CLEARED, "入住后不再是清空状态");
            h.assertTrue(level.getBlockEntity(c.one("vault")) instanceof RuinVaultBlockEntity, "宝库还在、不重置");
        } finally {
            cleanup(level, c);
        }
        h.succeed();
    }
}
