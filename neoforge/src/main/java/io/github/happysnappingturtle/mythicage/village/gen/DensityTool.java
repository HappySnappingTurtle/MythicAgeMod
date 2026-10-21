package io.github.happysnappingturtle.mythicage.village.gen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.core.village.gen.BiomeView;
import io.github.happysnappingturtle.mythicage.core.village.gen.TerrainSampler;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 密度验收工具（V0 §3.9，T13b §8）：不生成区块，按任意种子算出 {@code minecraft:villages} 结构集在范围内的全部候选区块，
 * 对每个候选做文明选择 + 选址 + 规划 + 校验，记录成败、原因与耗时；再在群系网格上抽希腊地域的边缘点，
 * 量到最近一座希腊村庄的距离。报告写到 {@code <存档>/mythic_age/density_<种子>.json}，由 tools/village_density.py 汇总判定。
 * <p>
 * 命令（开发环境）：{@code /mythicage dev villages density <种子> <半径区块>}
 */
public final class DensityTool {
    /** 群系网格间距（区块）。 */
    public static final int BIOME_STEP_CHUNKS = 4;
    public static final int MAX_EDGE_SAMPLES = 400;
    /** 成片地域的最小网格数（16 × 4 × 4 = 256 区块）。 */
    public static final int MIN_COMPONENT = 16;

    private DensityTool() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
        event.getDispatcher().register(Commands.literal("mythicage")
                .then(Commands.literal("dev").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("villages").then(Commands.literal("density")
                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, 512))
                                                .executes(DensityTool::run))))
                                .then(Commands.literal("bench").executes(DensityTool::bench))
                                .then(Commands.literal("audit").executes(DensityTool::audit))
                                .then(Commands.literal("anchors").executes(DensityTool::anchors)))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        long seed = LongArgumentType.getLong(ctx, "seed");
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        MinecraftServer server = ctx.getSource().getServer();
        try {
            JsonObject report = measure(server, seed, radius);
            Path out = server.getWorldPath(LevelResource.ROOT).resolve("mythic_age").resolve("density_" + seed + ".json");
            Files.createDirectories(out.getParent());
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            JsonObject s = report.getAsJsonObject("summary");
            ctx.getSource().sendSuccess(() -> Component.literal("密度 " + seed + "：" + s), false);
            return 1;
        } catch (IOException | RuntimeException e) {
            ctx.getSource().sendFailure(Component.literal("密度工具失败：" + e));
            return 0;
        }
    }

    /** 列出玩家所在村庄的主神与全部锚点坐标（实机取景用），写进审计日志。 */
    private static int anchors(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition());
        var plans = io.github.happysnappingturtle.mythicage.village.VillagePlans.get(level.getServer());
        var id = plans.at(level.dimension(), at.getX(), at.getZ());
        if (id.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("不在文明村庄里"));
            return 0;
        }
        var info = plans.byVillage(id.get()).orElseThrow().info();
        StringBuilder sb = new StringBuilder("anchors " + info.patron() + " center=" + info.center());
        info.anchors().forEach((k, v) -> sb.append(" ").append(k).append("=").append(v));
        info.plots().forEach(p -> sb.append(" plot:").append(p.kind()).append("=").append(p.box()));
        MythicAgeAuditLog.write(sb.toString());
        ctx.getSource().sendSuccess(() -> Component.literal("锚点已写入日志"), false);
        return 1;
    }

    /**
     * 埋地检查：玩家所在村庄的每座建筑，比较模板底面（地面层）与包围盒外 3 格一圈的实际地表；
     * 一圈地表的中位数比地面高 ≥ 2 格算「埋进地下」，同时报四边各自的高差。
     */
    private static int audit(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition());
        var plans = io.github.happysnappingturtle.mythicage.village.VillagePlans.get(level.getServer());
        var id = plans.at(level.dimension(), at.getX(), at.getZ());
        if (id.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("不在文明村庄里"));
            return 0;
        }
        var info = plans.byVillage(id.get()).orElseThrow().info();
        StringBuilder sb = new StringBuilder();
        int buried = 0;
        for (var b : info.built()) {
            int floor = b.minY() + 1; // 有地基层的模板：底面下一层是地基；没有地基层时偏差 1 格
            int[] side = new int[4];
            java.util.List<Integer> all = new ArrayList<>();
            int[][] ends = {{b.minX() - 3, b.minZ(), b.minX() - 3, b.maxZ()}, {b.maxX() + 3, b.minZ(), b.maxX() + 3, b.maxZ()},
                    {b.minX(), b.minZ() - 3, b.maxX(), b.minZ() - 3}, {b.minX(), b.maxZ() + 3, b.maxX(), b.maxZ() + 3}};
            final var self = b;
            for (int k = 0; k < 4; k++) {
                java.util.List<Integer> hs = new ArrayList<>();
                int[] e = ends[k];
                for (int x = e[0]; x <= e[2]; x += 2) {
                    for (int z = e[1]; z <= e[3]; z += 2) {
                        final int fx = x;
                        final int fz = z;
                        if (info.built().stream().anyMatch(o -> o != self && o.inflateXZ(1).containsXZ(fx, fz))) {
                            continue; // 落在别的建筑上
                        }
                        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                        net.minecraft.core.BlockPos.MutableBlockPos m = new net.minecraft.core.BlockPos.MutableBlockPos(x, y, z);
                        while (y > level.getMinBuildHeight() && (level.getBlockState(m).is(net.minecraft.tags.BlockTags.LOGS)
                                || level.getBlockState(m).canBeReplaced())) {
                            m.setY(--y);
                        }
                        hs.add(y + 1 - floor);
                    }
                }
                java.util.Collections.sort(hs);
                side[k] = hs.isEmpty() ? 0 : hs.get(hs.size() / 2);
                all.addAll(hs);
            }
            java.util.Collections.sort(all);
            int med = all.isEmpty() ? 0 : all.get(all.size() / 2);
            if (med >= 2) {
                buried++;
            }
            sb.append(String.format("[%d,%d,%d %dx%d 外圈中位 %+d 西%+d 东%+d 北%+d 南%+d] ", b.minX(), b.minY(), b.minZ(),
                    b.sizeX(), b.sizeZ(), med, side[0], side[1], side[2], side[3]));
        }
        int n = buried;
        int total = info.built().size();
        MythicAgeAuditLog.write(sb.toString());
        ctx.getSource().sendSuccess(() -> Component.literal("埋地 " + n + " / " + total + "（明细见日志）"), false);
        return 1;
    }

    /** 各种取地表高度方式的单次耗时（微秒）。 */
    private static int bench(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel level = server.overworld();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        NoiseBasedChunkGenerator noise = (NoiseBasedChunkGenerator) gen;
        RandomState rs = RandomState.create(noise.generatorSettings().value(),
                server.registryAccess().lookupOrThrow(Registries.NOISE), 12345L);
        int n = 200;
        long t0 = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += gen.getBaseHeight(i * 97, i * 61, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, rs);
        }
        double baseHeight = (System.nanoTime() - t0) / 1e3 / n;
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            sum += gen.getBaseColumn(i * 97, i * 61, level, rs).getBlock(64).hashCode();
        }
        double column = (System.nanoTime() - t0) / 1e3 / n;
        var f = rs.router().initialDensityWithoutJaggedness();
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            sum += (long) f.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(i * 97, 64, i * 61));
        }
        double density = (System.nanoTime() - t0) / 1e3 / n;
        TerrainSampler est = WorldgenViews.estimated(gen, level, rs);
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            sum += est.floor(i * 97 + 5, i * 61 + 3);
        }
        double estimate = (System.nanoTime() - t0) / 1e3 / n;
        int[] err = new int[n];
        int[] cal = new int[n];
        double signed = 0;
        for (int i = 0; i < n; i++) {
            int x = i * 97;
            int z = i * 61;
            int exact = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            err[i] = Math.abs(est.floor(x, z) - exact);
            signed += est.floor(x, z) - exact;
            // 局部校准：用 24 格外的 3 个点的精确高度求平均偏差，再修正本点
            double off = 0;
            for (int[] d : new int[][]{{24, 0}, {-12, 21}, {-12, -21}}) {
                off += est.floor(x + d[0], z + d[1]) - gen.getBaseHeight(x + d[0], z + d[1],
                        net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            }
            cal[i] = (int) Math.abs(Math.round(est.floor(x, z) - off / 3) - exact);
        }
        int[] perr = new int[n];
        long tp = 0;
        for (int i = 0; i < n; i++) {
            int x = i * 97;
            int z = i * 61;
            int exact = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            long a = System.nanoTime();
            int pr = WorldgenViews.precise(gen, level, rs, x, z, est.floor(x, z) + 9);
            tp += System.nanoTime() - a;
            perr[i] = Math.abs(pr - exact);
        }
        java.util.Arrays.sort(perr);
        double preciseUs = tp / 1e3 / n;
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("最终密度扫描 %.0f µs/列，误差 p50 %d p90 %d p99 %d",
                preciseUs, perr[n / 2], perr[n * 9 / 10], perr[n * 99 / 100])), false);
        java.util.Arrays.sort(err);
        java.util.Arrays.sort(cal);
        double bias = signed / n;
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("平均偏差 %.1f；局部校准后误差 p50 %d p90 %d p99 %d",
                bias, cal[n / 2], cal[n * 9 / 10], cal[n * 99 / 100])), false);
        long fsum = sum;
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("getBaseHeight %.0f µs，getBaseColumn %.0f µs，"
                + "initialDensity 单点 %.1f µs，估算地表 %.0f µs/列；估算误差 p50 %d p90 %d p99 %d 最大 %d（%d）",
                baseHeight, column, density, estimate, err[n / 2], err[n * 9 / 10], err[n * 99 / 100], err[n - 1], fsum % 7)), false);
        return 1;
    }

    public static JsonObject measure(MinecraftServer server, long seed, int radiusChunks) {
        ServerLevel level = server.overworld();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        if (!(gen instanceof NoiseBasedChunkGenerator noise)) {
            throw new IllegalStateException("主世界不是噪声生成器");
        }
        RandomState rs = RandomState.create(noise.generatorSettings().value(),
                server.registryAccess().lookupOrThrow(Registries.NOISE), seed);
        BiomeView biomes = WorldgenViews.biomes(gen, rs);
        StructureSet set = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.withDefaultNamespace("villages")));
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
        Structure s = server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("mythic_age", "greek_village")));
        CivVillageStructure greek = (CivVillageStructure) s;

        JsonArray cands = new JsonArray();
        List<int[]> villages = new ArrayList<>();
        int spacing = placement.spacing();
        int regions = radiusChunks / spacing + 1;
        for (int rx = -regions; rx <= regions; rx++) {
            for (int rz = -regions; rz <= regions; rz++) {
                ChunkPos c = placement.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                if (Math.abs(c.x) > radiusChunks || Math.abs(c.z) > radiusChunks) {
                    continue;
                }
                int x = c.getMiddleBlockX();
                int z = c.getMiddleBlockZ();
                if (!biomes.matches("#mythic_age:region/greek", x, z)) {
                    continue; // 不在希腊地域：结构集里别的条目（门控）才会尝试
                }
                List<String> problems = new ArrayList<>();
                long t0 = System.nanoTime();
                TerrainSampler terrain = WorldgenViews.terrain(gen, level, rs, x, z);
                java.util.Optional<VillagePlan> plan = greek.plan(x, z, biomes, terrain, problems);
                double ms = (System.nanoTime() - t0) / 1e6;
                JsonObject o = new JsonObject();
                o.addProperty("x", x);
                o.addProperty("z", z);
                o.addProperty("ok", plan.isPresent());
                o.addProperty("ms", Math.round(ms * 10) / 10.0);
                if (plan.isPresent()) {
                    o.addProperty("patron", plan.get().patron());
                    o.addProperty("buildings", plan.get().buildings().size());
                    villages.add(new int[]{plan.get().center().x(), plan.get().center().z()});
                } else {
                    o.addProperty("reason", problems.isEmpty() ? "?" : problems.get(problems.size() - 1));
                }
                cands.add(o);
            }
        }
        // 群系网格：希腊地域面积与边缘点
        int step = BIOME_STEP_CHUNKS;
        int n = radiusChunks / step;
        boolean[][] greekCell = new boolean[2 * n + 1][2 * n + 1];
        int regionChunks = 0;
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                boolean g = biomes.matches("#mythic_age:region/greek", i * step * 16 + 8, j * step * 16 + 8);
                greekCell[i + n][j + n] = g;
                regionChunks += g ? step * step : 0;
            }
        }
        // 连通片：只统计成片的地域（≥ MIN_COMPONENT 个网格）的边缘；零碎小块（石岸、沼泽碎片）另计
        int size = 2 * n + 1;
        int[][] comp = new int[size][size];
        List<Integer> compSize = new ArrayList<>();
        compSize.add(0);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (greekCell[i][j] && comp[i][j] == 0) {
                    int id = compSize.size();
                    int count = 0;
                    java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                    q.add(new int[]{i, j});
                    comp[i][j] = id;
                    while (!q.isEmpty()) {
                        int[] c = q.poll();
                        count++;
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0];
                            int b = c[1] + d[1];
                            if (a >= 0 && b >= 0 && a < size && b < size && greekCell[a][b] && comp[a][b] == 0) {
                                comp[a][b] = id;
                                q.add(new int[]{a, b});
                            }
                        }
                    }
                    compSize.add(count);
                }
            }
        }
        List<int[]> edges = new ArrayList<>();
        int fragments = 0;
        for (int i = 1; i < 2 * n; i++) {
            for (int j = 1; j < 2 * n; j++) {
                if (greekCell[i][j] && (!greekCell[i - 1][j] || !greekCell[i + 1][j] || !greekCell[i][j - 1] || !greekCell[i][j + 1])) {
                    if (compSize.get(comp[i][j]) >= MIN_COMPONENT) {
                        edges.add(new int[]{(i - n) * step * 16 + 8, (j - n) * step * 16 + 8});
                    } else {
                        fragments++;
                    }
                }
            }
        }
        java.util.Collections.shuffle(edges, new java.util.Random(seed));
        JsonArray edgeDist = new JsonArray();
        for (int[] e : edges.subList(0, Math.min(MAX_EDGE_SAMPLES, edges.size()))) {
            double best = Double.MAX_VALUE;
            for (int[] v : villages) {
                best = Math.min(best, Math.hypot(v[0] - e[0], v[1] - e[1]));
            }
            edgeDist.add(villages.isEmpty() ? -1 : Math.round(best));
        }
        JsonObject summary = new JsonObject();
        summary.addProperty("candidates", cands.size());
        summary.addProperty("villages", villages.size());
        summary.addProperty("regionChunks", regionChunks);
        summary.addProperty("chunksPerVillage", villages.isEmpty() ? -1 : regionChunks / villages.size());
        summary.addProperty("edgeFragments", fragments);
        JsonObject root = new JsonObject();
        root.addProperty("seed", seed);
        root.addProperty("radiusChunks", radiusChunks);
        root.add("summary", summary);
        root.add("candidates", cands);
        root.add("edgeDistances", edgeDist);
        return root;
    }

    private static final class MythicAgeAuditLog {
        static void write(String s) {
            io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("[village audit] {}", s);
        }
    }
}
