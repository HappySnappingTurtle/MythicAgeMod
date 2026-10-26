package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.structure.LabyrinthStructure;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import io.github.happysnappingturtle.mythicage.structure.StructureSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * T22 阶段 ③：克诺索斯迷宫的布局与模板（单元测试用例文档 TC-T22-60～65）。迷宫摆在远处的槽位（x = 80000 + n·256），
 * 地面 y = 40、台阶 24 级（地表 64）；地表残迹那一块先铺平，其余用自然地形（迷宫模块整块实心，自己就是外壳）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LabyrinthGameTests {
    private static final String BATCH = "labyrinth";
    private static final String T = GameTestTemplates.EMPTY_3X3;
    private static final AtomicInteger SLOT = new AtomicInteger();
    static final int FLOOR = 40;
    static final int DEPTH = 24;
    /** 上一次 {@link #place} 里单个区块放置的最长耗时（TC-T22-81）。 */
    static long lastMaxChunkNanos;

    private LabyrinthGameTests() {
    }

    record Placed(StructureStart start, LabyrinthStructure.Site site, StructureInfoPiece.Info info) {
    }

    static LabyrinthStructure structure(ServerLevel level) {
        return (LabyrinthStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "knossos")));
    }

    /** 摆一座：water = true 时先在迷宫范围里放几处水（测外壳与防水）。 */
    static Placed place(GameTestHelper h, long seed, boolean water) {
        ServerLevel level = h.getLevel();
        int gx = 80000 + SLOT.getAndIncrement() * 256;
        int gz = 80000;
        LabyrinthStructure.Site site = new LabyrinthStructure.Site(gx, gz, FLOOR, DEPTH);
        for (int cx = (gx - 16) >> 4; cx <= (gx + 80) >> 4; cx++) {
            for (int cz = (gz - 16) >> 4; cz <= (gz + 120) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        int surface = FLOOR + DEPTH;
        // 地表残迹那一块：地面铺石头、上面清空（台阶顶在 z = gz + 64 + 24 附近）
        for (BlockPos p : BlockPos.betweenClosed(gx - 12, surface - 1, gz + 64 + DEPTH - 20, gx + 20, surface - 1, gz + 64 + DEPTH + 16)) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(gx - 12, surface, gz + 64 + DEPTH - 20, gx + 20, surface + 12, gz + 64 + DEPTH + 16)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        if (water) {
            for (int[] w : new int[][]{{10, 10}, {30, 30}, {50, 60}, {3, 60}}) {
                level.setBlock(new BlockPos(gx + w[0], FLOOR + 1, gz + w[1]), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + w[0], FLOOR - 1, gz + w[1]), Blocks.LAVA.defaultBlockState(), 2);
            }
        }
        StructureStart start = structure(level).startForTest(level, site, seed);
        BoundingBox bb = start.getBoundingBox();
        ChunkPos a = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos b = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        lastMaxChunkNanos = 0;
        ChunkPos.rangeClosed(a, b).forEach(c -> {
            level.getChunk(c.x, c.z);
            long t0 = System.nanoTime();
            start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(),
                            level.getMaxBuildHeight(), c.getMaxBlockZ()), c);
            lastMaxChunkNanos = Math.max(lastMaxChunkNanos, System.nanoTime() - t0);
        });
        StructureInfoPiece.Info info = StructureRegistration.info(start).orElseThrow(() -> new GameTestAssertException("缺元数据件"));
        return new Placed(start, site, info);
    }

    static void cleanup(ServerLevel level, Placed p) {
        StructureSites.get(level.getServer()).forgetForTest(StructureRegistration.key(level, p.start()));
    }

    static BlockPos one(Placed p, String name) {
        if (p.info().anchors(name).isEmpty()) {
            throw new GameTestAssertException("缺锚点 " + name);
        }
        return p.info().anchors(name).get(0);
    }

    /** TC-T22-60：一座的全部锚点都在（入口、线团、大厅安全点与 Boss 出生点、8 柱、A / B 墙槽、牛首门、宝库与门槽）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void labyrinth_anchors(GameTestHelper h) {
        Placed p = place(h, 1, false);
        StructureInfoPiece.Info i = p.info();
        h.assertTrue(i.kind().equals("greek:knossos"), "种类");
        for (String a : new String[]{"entrance", "thread", "safe_return", "boss_origin", "gate", "vault", "vault_door"}) {
            h.assertTrue(!i.anchors(a).isEmpty(), "锚点 " + a);
        }
        h.assertTrue(i.anchors("pillar").size() == 8, "8 柱：" + i.anchors("pillar").size());
        h.assertTrue(i.anchors("wall_a").size() == 14 && i.anchors("wall_b").size() == 14, "A / B 墙槽各 14 格");
        h.assertTrue(i.anchors("quest").size() == 1, "教学柱廊 1 间");
        h.assertTrue(i.anchors("safe_return").size() == 2, "大厅安全点 + 休整点");
        cleanup(h.getLevel(), p);
        h.succeed();
    }

    /** TC-T22-61：从地表入口走得到线团祭坛、牛首门前、大厅安全点、休整点（合同「入口可达」）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void labyrinth_reachable(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (long seed : new long[]{2, 3}) {
            Placed p = place(h, seed, false);
            BlockPos entrance = one(p, "entrance");
            h.assertTrue(AbandonedTempleGameTests.standable(level, entrance), "地表入口站得住：" + entrance);
            h.assertTrue(reachable(level, entrance, one(p, "thread")), seed + " 走得到线团祭坛");
            h.assertTrue(reachable(level, entrance, one(p, "gate")), seed + " 走得到牛首门");
            for (BlockPos s : p.info().anchors("safe_return")) {
                h.assertTrue(reachable(level, entrance, s), seed + " 走得到安全点 " + s);
            }
            cleanup(level, p);
        }
        h.succeed();
    }

    /** 迷宫比神殿大得多：可达性搜索上限放到 60000 格。 */
    static boolean reachable(ServerLevel level, BlockPos from, BlockPos to) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty() && seen.size() < 60000) {
            BlockPos p = queue.poll();
            if (Math.abs(p.getY() - to.getY()) <= 1 && Math.abs(p.getX() - to.getX()) + Math.abs(p.getZ() - to.getZ()) <= 1) {
                return true;
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n = p.relative(d).above(dy);
                    if (dy == 1 && !level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty()) {
                        continue;
                    }
                    if (seen.add(n) && AbandonedTempleGameTests.standable(level, n)) {
                        queue.add(n);
                    }
                }
            }
        }
        return false;
    }

    /** TC-T22-62、63：大厅按 BO-GR-01：609 格可走（617 − 8 柱）、8 根柱顶到穹顶、穹顶底面高 7；宝库门槽封着，从大厅走不进宝库房。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void labyrinth_hall_matches_arena(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Placed p = place(h, 4, false);
        JsonObject arena = arena(h);
        int cx = p.site().gx() + 32;
        int cz = p.site().gz() + 32;
        int walk = 0;
        for (JsonElement e : arena.getAsJsonArray("floor_cells")) {
            JsonArray xz = e.getAsJsonArray();
            BlockPos at = new BlockPos(cx + xz.get(0).getAsInt(), FLOOR, cz + xz.get(1).getAsInt());
            h.assertTrue(!level.getBlockState(at.below()).isAir(), "地面实心：" + at);
            h.assertTrue(!level.getBlockState(at.above(7)).isAir(), "穹顶在 7：" + at);
            if (level.getBlockState(at).isAir()) {
                walk++;
            }
        }
        h.assertTrue(walk == 609, "可走 609 格：" + walk);
        for (BlockPos pillar : p.info().anchors("pillar")) {
            for (int dy = 0; dy < 7; dy++) {
                h.assertTrue(!level.getBlockState(pillar.above(dy)).isAir(), "柱子顶到穹顶（7 格）：" + pillar.above(dy));
            }
        }
        BlockPos door = one(p, "vault_door");
        h.assertTrue(!level.getBlockState(door).isAir(), "宝库门槽封着：" + door);
        h.assertTrue(!reachable(level, one(p, "safe_return"), one(p, "vault")), "从大厅走不进宝库房");
        cleanup(level, p);
        h.succeed();
    }

    /** TC-T22-64、65：迷宫层里没有液体（槽位里事先放了水与岩浆）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void labyrinth_no_fluids(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Placed p = place(h, 5, true);
        int gx = p.site().gx();
        int gz = p.site().gz();
        for (BlockPos q : BlockPos.betweenClosed(gx, FLOOR - 2, gz, gx + 63, FLOOR + 8, gz + 63)) {
            if (!level.getFluidState(q).isEmpty()) {
                throw new GameTestAssertException("迷宫里有液体：" + q);
            }
        }
        cleanup(level, p);
        h.succeed();
    }

    /** TC-T22-81：单区块放置耗时（模板先预热一次，再量第二座）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void labyrinth_placement_cost(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        cleanup(level, place(h, 6, false));
        Placed p = place(h, 7, false);
        double ms = lastMaxChunkNanos / 1e6;
        io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("迷宫单区块放置最长 {} ms", String.format("%.1f", ms));
        h.assertTrue(ms < 50, "单区块放置应 < 50 ms，实际 " + ms);
        cleanup(level, p);
        h.succeed();
    }

    static JsonObject arena(GameTestHelper h) {
        try (var in = LabyrinthGameTests.class.getResourceAsStream("/data/mythic_age/mythic_age/arena/greek_knossos.json")) {
            if (in == null) {
                throw new GameTestAssertException("缺大厅坐标数据");
            }
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("读大厅坐标数据失败：" + e.getMessage());
        }
    }
}
