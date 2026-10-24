package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OliveLeavesBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.TallWorkstationBlock;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.LandJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.NaturalResources;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T19 阶段 ② 希腊生产链（TC-T19-60～70）。用正式的希腊村庄数据（village_civ/greek.json 与配方），场地 17 × 16 × 36，地面 y = 1。
 * 居民的走路与动作用 {@link ResidentEntity#fastForwardForTest} 跳过（跳过的动作段照常触发中间效果）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GreekVillageGameTests {
    private static final String BATCH = "greek_village";
    /** 会在场地外放方块、或推进 MC 日的用例各占一批。 */
    private static final String SOLO_1 = "greek_village_solo_1";
    private static final String SOLO_2 = "greek_village_solo_2";
    private static final String SOLO_3 = "greek_village_solo_3";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String GREEK = "greek";
    private static final String FARMER = "greek:farmer", POTTER = "greek:potter", LABORER = "greek:laborer";

    private GreekVillageGameTests() {
    }

    private static Pos abs(GameTestHelper h, int x, int y, int z) {
        return VillageWorkGameTests.abs(h, x, y, z);
    }

    /** 希腊村庄；已建范围只是中心附近一小块（林地、采石点的搜索会排除已建范围）。 */
    private static VillageWorkGameTests.TV village(GameTestHelper h, Map<String, List<Pos>> anchors) {
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Box built = new Box(o.getX() + 6, o.getY() + 1, o.getZ(), o.getX() + 10, o.getY() + 4, o.getZ() + 4);
        return VillageWorkGameTests.village(h, GREEK, anchors, built);
    }

    private static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageWorkGameTests.state(h, tv);
    }

    private static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv) {
        VillageWorkGameTests.cleanup(h, tv);
    }

    private static Map<String, List<Pos>> anchors(Object... kv) {
        Map<String, List<Pos>> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.computeIfAbsent((String) kv[i], k -> new ArrayList<>()).add((Pos) kv[i + 1]);
        }
        return m;
    }

    /** 橄榄树：(x, 2, z) 起三格树干，四周一圈树叶（自然树叶，非玩家放置）；ripe 片结熟果。 */
    private static void oliveTree(GameTestHelper h, int x, int z, int ripe) {
        for (int y = 2; y <= 4; y++) {
            h.setBlock(new BlockPos(x, y, z), GreekAgriculture.OLIVE_LOG.get());
        }
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockState leaf = GreekAgriculture.OLIVE_LEAVES.get().defaultBlockState()
                        .setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1)
                        .setValue(OliveLeavesBlock.FRUIT, n++ < ripe ? OliveLeavesBlock.RIPE : 0);
                h.setBlock(new BlockPos(x + dx, 4, z + dz), leaf);
            }
        }
    }

    private static int ripeLeaves(GameTestHelper h, int x, int z) {
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState s = h.getBlockState(new BlockPos(x + dx, 4, z + dz));
                if (s.getBlock() instanceof OliveLeavesBlock && s.getValue(OliveLeavesBlock.FRUIT) == OliveLeavesBlock.RIPE) {
                    n++;
                }
            }
        }
        return n;
    }

    // ───────────── 数据 ─────────────

    /** 数据自检：希腊数据里的物品、工作站方块都存在；户外 / 室内标记符合「下雨时户外暂停、有棚的照常」。 */
    @GameTest(template = T, batch = BATCH)
    public static void greek_data_references_resolve(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        CivVillageData d = VillageData.civ(GREEK).orElseThrow();
        List<String> missing = new ArrayList<>();
        java.util.function.Consumer<String> item = id -> {
            if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))) {
                missing.add(id);
            }
        };
        for (VillageRecipe r : d.recipes().values()) {
            r.inputs().keySet().forEach(item);
            r.outputs().keySet().forEach(item);
            if (!r.station().contains(":") || (!r.station().startsWith("anchor:") && !r.station().startsWith("point:")
                    && !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(r.station())))) {
                missing.add("station " + r.station());
            }
        }
        d.points().values().forEach(k -> k.outputs().keySet().forEach(item));
        d.fruits().values().forEach(k -> k.expected().keySet().forEach(item));
        d.initialStock().keySet().forEach(item);
        d.rations().portions().keySet().forEach(item);
        d.fuel().perUnit().keySet().forEach(item);
        h.assertTrue(missing.isEmpty(), "数据引用了不存在的东西：" + missing);
        h.assertTrue(d.recipes().size() >= 19, "配方数：" + d.recipes().size());
        h.assertTrue(!d.recipes().get("greek:fire_tile").outdoor() && !d.recipes().get("greek:shape_tile").outdoor(), "窑与陶轮在棚下");
        h.assertTrue(d.points().get("quarry").outdoor() && d.points().get("clay").outdoor(), "采石、挖黏土在户外");
        h.succeed();
    }

    // ───────────── 产出链 ─────────────

    /** TC-T19-60：打橄榄：熟果的树叶变回未结果，橄榄搬回仓库入库（每片熟叶 1–2 颗）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void olive_picking(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        oliveTree(h, 4, 8, 5);
        VillageWorkGameTests.TV tv = village(h, anchors("fruit_tree", abs(h, 4, 2, 8), "storage_keeper", abs(h, 6, 2, 8)));
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, LABORER, 5, 8);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals(LandJobTypes.PICK)).orElse(false), "派了采果单：" + r.job());
        r.fastForwardForTest();
        h.succeedWhen(() -> {
            int olives = state(h, tv).stock.count("mythic_age:olive");
            h.assertTrue(olives >= 5 && olives <= 10, "橄榄 5–10：" + olives);
            h.assertTrue(ripeLeaves(h, 4, 8) == 0, "熟果都摘了");
            cleanup(h, tv);
        });
    }

    /** TC-T19-61：没熟的树不派单。 */
    @GameTest(template = T, batch = BATCH)
    public static void unripe_tree_not_picked(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        oliveTree(h, 4, 8, 0);
        VillageWorkGameTests.TV tv = village(h, anchors("fruit_tree", abs(h, 4, 2, 8)));
        try {
            ResidentEntity r = VillageWorkGameTests.resident(h, tv, LABORER, 5, 8);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().map(j -> !j.type().equals(LandJobTypes.PICK)).orElse(true), "不派采果单");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-62：压榨：橄榄 4 + 陶制构件 1 → 橄榄油 1（工作站在站位旁边一格）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void olive_press(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        h.setBlock(new BlockPos(5, 2, 11), GreekAgriculture.OLIVE_PRESS.get());
        VillageWorkGameTests.TV tv = village(h, anchors("workstation", abs(h, 5, 2, 10)));
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, FARMER, 5, 9);
        VillageWorkGameTests.takeOver(h, tv);
        VillageEconomy.State s = state(h, tv);
        int comp = s.stock.count("mythic_age:ceramic_component");
        s.stock.deposit("mythic_age:olive", 4);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals(BasicJobTypes.CRAFT)).orElse(false), "派了压榨单：" + r.job());
        r.fastForwardForTest();
        h.succeedWhen(() -> {
            h.assertTrue(s.stock.count("mythic_age:olive_oil") == 1, "橄榄油 1：" + s.stock.items());
            h.assertTrue(s.stock.count("mythic_age:olive") == 0 && s.stock.count("mythic_age:ceramic_component") == comp - 1,
                    "扣了橄榄 4、构件 1");
            cleanup(h, tv);
        });
    }

    /** TC-T19-63：黏土 → 拉坯 → 烧窑（+燃料）→ 陶制构件；燃料扣 1 单位。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 600)
    public static void clay_to_ceramic(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        h.setBlock(new BlockPos(3, 2, 14), Blocks.CLAY);
        h.setBlock(new BlockPos(5, 2, 15), GreekVillageBlocks.POTTERY_WHEEL.get());
        BlockState kiln = GreekVillageBlocks.POTTERY_KILN.get().defaultBlockState().setValue(TallWorkstationBlock.FACING, Direction.NORTH);
        h.setBlock(new BlockPos(8, 2, 15), kiln.setValue(TallWorkstationBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(new BlockPos(8, 3, 15), kiln.setValue(TallWorkstationBlock.HALF, DoubleBlockHalf.UPPER));
        VillageWorkGameTests.TV tv = village(h, anchors("clay", abs(h, 3, 2, 14), "workstation", abs(h, 5, 2, 14),
                "workstation", abs(h, 8, 2, 14)));
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, POTTER, 5, 13);
        VillageWorkGameTests.takeOver(h, tv);
        VillageEconomy.State s = state(h, tv);
        int start = s.stock.count("mythic_age:ceramic_component");
        s.stock.deposit("minecraft:charcoal", 1);
        h.succeedWhen(() -> {
            r.fastForwardForTest();
            h.assertTrue(s.stock.count("mythic_age:ceramic_component") >= start + 4, "多了 4 个陶制构件：" + s.stock.items());
            h.assertTrue(s.stock.count("minecraft:charcoal") == 0, "燃料用掉 1 单位");
            cleanup(h, tv);
        });
    }

    private static void wheat(GameTestHelper h, int x0, int z, int n, boolean ripe) {
        for (int i = 0; i < n; i++) {
            h.setBlock(new BlockPos(x0 + i, 1, z), Blocks.FARMLAND);
            h.setBlock(new BlockPos(x0 + i, 2, z), ((CropBlock) Blocks.WHEAT).getStateForAge(ripe ? 7 : 0));
        }
    }

    /** TC-T19-64：收麦 → 打谷：打完谷才入库，扣掉补种的种子；麦田回到幼苗。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void wheat_harvest_and_thresh(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        wheat(h, 4, 20, 4, true);
        List<Pos> crops = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            crops.add(abs(h, 4 + i, 2, 20));
        }
        Map<String, List<Pos>> a = anchors("thresh", abs(h, 5, 2, 23));
        a.put("crop", crops);
        VillageWorkGameTests.TV tv = village(h, a);
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, FARMER, 5, 21);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals(BasicJobTypes.HARVEST)).orElse(false), "派了收麦单：" + r.job());
        r.fastForwardForTest();
        h.succeedWhen(() -> {
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.stock.count("minecraft:wheat") == 4, "小麦 4：" + s.stock.items());
            for (int i = 0; i < 4; i++) {
                BlockState c = h.getBlockState(new BlockPos(4 + i, 2, 20));
                h.assertTrue(c.is(Blocks.WHEAT) && !((CropBlock) Blocks.WHEAT).isMaxAge(c), "回到幼苗");
            }
            cleanup(h, tv);
        });
    }

    /** TC-T19-65：打谷中途打断：不产出；割过的麦田保持割后的状态（不复原、不再结算）。 */
    @GameTest(template = T, batch = BATCH)
    public static void thresh_interrupted(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        wheat(h, 4, 20, 2, true);
        Map<String, List<Pos>> a = anchors("thresh", abs(h, 5, 2, 23));
        a.put("crop", List.of(abs(h, 4, 2, 20), abs(h, 5, 2, 20)));
        VillageWorkGameTests.TV tv = village(h, a);
        try {
            ResidentEntity r = VillageWorkGameTests.resident(h, tv, FARMER, 5, 21);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().isPresent(), "派了单");
            for (int i = 0; i < 400 && r.job().isPresent() && r.step() < 2; i++) {
                r.tickJobForTest();
            }
            h.assertTrue(r.step() >= 2, "已割完、在去打谷场的路上：步 " + r.step());
            r.abortJob(false);
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.stock.count("minecraft:wheat") == 0, "不产出：" + s.stock.items());
            h.assertTrue(!((CropBlock) Blocks.WHEAT).isMaxAge(h.getBlockState(new BlockPos(4, 2, 20))), "麦田不复原");
            h.assertTrue(VillageScheduler.runtime(tv.id()).pending().isEmpty(), "途中物品清掉");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-66：踩葡萄 → 酒瓮发酵 2 日 → 取酒：葡萄酒 2；重启后发酵不重复完成。 */
    @GameTest(template = T, batch = SOLO_1, timeoutTicks = 300)
    public static void wine_ferments(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        h.setBlock(new BlockPos(5, 2, 11), GreekVillageBlocks.PITHOS.get());
        VillageWorkGameTests.TV tv = village(h, anchors("workstation", abs(h, 5, 2, 10)));
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        reg.update(reg.byId(tv.id()).orElseThrow().withStage(Age.CLASSICAL));
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, FARMER, 5, 9);
        VillageWorkGameTests.takeOver(h, tv);
        VillageEconomy.State s = state(h, tv);
        s.stock.deposit("mythic_age:grape_must", 1);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.ref().startsWith("greek:ferment")).orElse(false), "派了装瓮单：" + r.job());
        r.fastForwardForTest();
        r.tickJobForTest();
        h.assertTrue(s.vessels.size() == 1 && s.stock.count("mythic_age:grape_must") == 0, "装进酒瓮");
        VillageEconomy eco = VillageEconomy.get(h.getLevel().getServer());
        eco.replaceForTest(tv.id(), VillageEconomy.roundTripForTest(eco, h.getLevel().registryAccess()));
        VillageEconomy.State s2 = state(h, tv);
        h.assertTrue(s2.vessels.size() == 1 && s2.stock.escrowed("mythic_age:grape_must") == 1, "重启后酒瓮与托管都在");
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> !j.type().equals(BasicJobTypes.COLLECT)).orElse(true), "没到日子不取");
        MythDays.advanceForTest(2);
        try {
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().map(j -> j.type().equals(BasicJobTypes.COLLECT)).orElse(false), "到日子取酒：" + r.job());
            r.fastForwardForTest();
            r.tickJobForTest();
            h.assertTrue(s2.stock.count("mythic_age:wine") == 2 && s2.vessels.isEmpty(), "葡萄酒 2：" + s2.stock.items());
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(s2.stock.count("mythic_age:wine") == 2, "不重复完成");
        } finally {
            MythDays.advanceForTest(-2);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-67：伐登记林地的一株：只砍这株，补种树苗；玩家放的树（树叶是玩家放置的）不登记、不动。 */
    @GameTest(template = T, batch = SOLO_2, timeoutTicks = 200)
    public static void fell_registered_tree(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        VillageWorkGameTests.TV tv = village(h, anchors("storage_keeper", abs(h, 8, 2, 12)));
        // 天然树：土上 4 格橡木 + 非玩家放置的树叶
        h.setBlock(new BlockPos(12, 1, 20), Blocks.DIRT);
        for (int y = 2; y <= 5; y++) {
            h.setBlock(new BlockPos(12, y, 20), Blocks.OAK_LOG);
        }
        h.setBlock(new BlockPos(12, 6, 20), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1));
        // 玩家的树：树叶是玩家放置的
        h.setBlock(new BlockPos(3, 1, 30), Blocks.DIRT);
        for (int y = 2; y <= 4; y++) {
            h.setBlock(new BlockPos(3, y, 30), Blocks.OAK_LOG);
        }
        h.setBlock(new BlockPos(3, 5, 30), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        ResidentEntity r = VillageWorkGameTests.resident(h, tv, LABORER, 12, 19);
        VillageWorkGameTests.takeOver(h, tv);
        VillageEconomy.State s = state(h, tv);
        BlockPos natural = h.absolutePos(new BlockPos(12, 2, 20));
        BlockPos player = h.absolutePos(new BlockPos(3, 2, 30));
        h.assertTrue(s.points.values().stream().anyMatch(p -> p.kind().equals(NaturalResources.WOODLAND)
                && VillageWorkGameTests.abs(h, 12, 2, 20).equals(p.pos())), "天然树登记了：" + s.points.keySet());
        h.assertTrue(s.points.values().stream().noneMatch(p -> p.pos().x() == player.getX() && p.pos().z() == player.getZ()),
                "玩家的树没登记");
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals(LandJobTypes.FELL)).orElse(false), "派了伐木单：" + r.job());
        r.fastForwardForTest();
        h.succeedWhen(() -> {
            h.assertTrue(s.stock.count("minecraft:oak_log") == 4, "原木 4：" + s.stock.items());
            h.assertTrue(h.getLevel().getBlockState(natural).is(Blocks.OAK_SAPLING), "补种了树苗");
            h.assertTrue(h.getBlockState(new BlockPos(3, 2, 30)).is(Blocks.OAK_LOG), "玩家的树不动");
            cleanup(h, tv);
        });
    }

    /** TC-T19-68（D6）：附近有露头的大理石就登记它；没有就在资源区放一处 3×3×2 露头。 */
    @GameTest(template = T, batch = SOLO_3)
    public static void quarry_found_or_placed(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        h.setBlock(new BlockPos(13, 2, 25), GreekAgriculture.OLIVE_LOG.get()); // 无关方块
        h.setBlock(new BlockPos(14, 2, 28), io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.MARBLE.get());
        VillageWorkGameTests.TV tv = village(h, Map.of());
        VillageEconomy.State s = state(h, tv);
        try {
            VillageWorkGameTests.takeOver(h, tv);
            ResourcePoint q = s.points.get("quarry_0");
            BlockPos mp = h.absolutePos(new BlockPos(14, 2, 28));
            h.assertTrue(q != null && q.pos().equals(abs(h, 14, 2, 28)), "登记了露头大理石：" + s.points.keySet()
                    + " 高度图 " + h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, mp.getX(), mp.getZ())
                    + " 方块 y " + mp.getY() + " = " + h.getLevel().getBlockState(mp) + " 中心 " + tv.village().bell().pos()
                    + " 已加载 " + h.getLevel().isLoaded(mp));
            // 没有露头：拆掉后清掉登记，再找一次 → 放一处 3×3×2
            h.setBlock(new BlockPos(14, 2, 28), Blocks.AIR);
            s.points.remove("quarry_0");
            CivVillageData.NaturalPoint full = VillageData.civ(GREEK).orElseThrow().naturalPoints().get("quarry");
            // 只在本场地附近找（20 格，露头从 14 格起放）：同一批并行的用例会在隔壁场地放露头，48 格的正式半径会找到别人的（场地排布变了才暴露）
            CivVillageData.NaturalPoint np = new CivVillageData.NaturalPoint(full.kind(), full.block(), 20, full.outcrop());
            h.assertTrue(NaturalResources.registerNaturalForTest(h.getLevel(), tv.id(), np), "放了露头");
            ResourcePoint o = s.points.get("quarry" + NaturalResources.OUTCROP);
            h.assertTrue(o != null, "露头登记了：" + s.points.keySet() + " " + s.points.values().stream()
                    .map(x -> x.id() + "@" + x.pos() + "=" + h.getLevel().getBlockState(new BlockPos(x.pos().x(), x.pos().y(), x.pos().z())))
                    .toList() + " 场地原点 " + h.absolutePos(BlockPos.ZERO));
            BlockPos top = new BlockPos(o.pos().x(), o.pos().y(), o.pos().z());
            int marble = 0;
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, -1, -1), top.offset(1, 0, 1))) {
                if (h.getLevel().getBlockState(p).is(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.MARBLE.get())) {
                    marble++;
                }
            }
            h.assertTrue(marble == 18, "3×3×2 = 18 块：" + marble);
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, -1, -1), top.offset(1, 0, 1))) {
                h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /**
     * TC-T19-70：自主启动（11 号 §7.1）。按生成村庄的规模摆一座远古希腊村（10 人：农务者 2、陶工 1、劳力 7；橄榄树 8、麦田 80 格、
     * 黏土坑、采石点、林地 3 株、压榨器、陶轮、陶窑、2 个灶），不捐物，用离线补算推进 2 × 3 个 MC 日：
     * 板材 ≥ 64、石材 ≥ 48、黏土 ≥ 24、口粮 ≥ 24。
     */
    @GameTest(template = T, batch = BATCH)
    public static void archaic_village_reaches_s1(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        List<Pos> crops = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            crops.add(abs(h, 1 + i % 14, 2, 30 + i / 14 % 4));
        }
        a.put("crop", crops);
        List<Pos> trees = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            trees.add(abs(h, 1 + i, 2, 26));
        }
        a.put("fruit_tree", trees);
        a.put("clay", List.of(abs(h, 2, 2, 20)));
        a.put("storage_keeper", List.of(abs(h, 8, 2, 12)));
        // 工作站：站位 → 旁边一格的方块
        h.setBlock(new BlockPos(3, 2, 15), GreekAgriculture.OLIVE_PRESS.get());
        h.setBlock(new BlockPos(6, 2, 15), GreekVillageBlocks.POTTERY_WHEEL.get());
        BlockState kiln = GreekVillageBlocks.POTTERY_KILN.get().defaultBlockState();
        h.setBlock(new BlockPos(9, 2, 15), kiln.setValue(TallWorkstationBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(new BlockPos(9, 3, 15), kiln.setValue(TallWorkstationBlock.HALF, DoubleBlockHalf.UPPER));
        a.put("workstation", List.of(abs(h, 3, 2, 14), abs(h, 6, 2, 14), abs(h, 9, 2, 14)));
        h.setBlock(new BlockPos(12, 2, 15), Blocks.CAMPFIRE);
        h.setBlock(new BlockPos(14, 2, 15), Blocks.CAMPFIRE);
        List<Pos> homes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            homes.add(abs(h, 11 + i % 5, 2, 13 + i / 5 * 4));
        }
        a.put("home", homes);
        h.setBlock(new BlockPos(14, 2, 22), io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.MARBLE.get());
        VillageWorkGameTests.TV tv = village(h, a);
        try {
            CivVillageData d = VillageData.civ(GREEK).orElseThrow();
            List<String> profs = new ArrayList<>(List.of(FARMER, FARMER, POTTER));
            while (profs.size() < 10) {
                profs.add(LABORER);
            }
            for (int i = 0; i < 10; i++) {
                BlockPos at = new BlockPos(homes.get(i).x(), homes.get(i).y(), homes.get(i).z());
                Residents.spawn(h.getLevel(), tv.village(), d, profs.get(i), at, at, false).orElseThrow().discard();
            }
            VillageWorkGameTests.takeOver(h, tv);
            VillageEconomy.State s = state(h, tv);
            // 林地 3 株（树干在场地边上）
            for (int i = 0; i < 3; i++) {
                BlockPos log = h.absolutePos(new BlockPos(15, 2, 4 + i * 4));
                h.getLevel().setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
                String id = NaturalResources.WOODLAND + "_t" + i;
                s.points.put(id, new ResourcePoint(id, NaturalResources.WOODLAND,
                        new Pos(log.getX(), log.getY(), log.getZ()), 1, 1, 0, MythDays.today(h.getLevel().getServer())));
            }
            for (int round = 0; round < 2; round++) {
                VillageScheduler.forceRunningForTest(tv.id(), false);
                VillageScheduler.processNowForTest(h.getLevel(), tv.id());
                s.lastRealTick = WorldClock.now(h.getLevel().getServer()) - 72000;
                VillageScheduler.forceRunningForTest(tv.id(), true);
                VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            }
            int planks = s.stock.items().entrySet().stream().filter(e -> e.getKey().endsWith("_planks"))
                    .mapToInt(Map.Entry::getValue).sum();
            int stone = s.stock.count("mythic_age:marble") + s.stock.count("mythic_age:polygonal_marble");
            int clay = s.stock.count("minecraft:clay_ball");
            double food = d.rations().total(s.stock);
            String all = s.stock.items().toString();
            h.assertTrue(planks >= 64, "板材 ≥ 64：" + planks + " " + all);
            h.assertTrue(stone >= 48, "石材 ≥ 48：" + stone + " " + all);
            h.assertTrue(clay >= 24, "黏土 ≥ 24：" + clay + " " + all);
            h.assertTrue(food >= 24, "口粮 ≥ 24：" + food + " " + all);
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }
}
