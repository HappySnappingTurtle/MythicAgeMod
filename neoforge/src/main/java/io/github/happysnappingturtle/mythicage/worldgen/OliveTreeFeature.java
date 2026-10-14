package io.github.happysnappingturtle.mythicage.worldgen;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 橄榄树（T6b 设计文档 §3.2）：主干 2–3 格且略斜，顶端分出 2–3 根斜向外伸的侧枝，枝端各一团扁平树冠。
 * 总高 5–7、冠幅 5–7。只替换空气与可替换方块；任何一格放不下时整棵不生成（树苗保持原样）。
 * 世界生成与树苗共用这一份逻辑。
 */
public class OliveTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int[][] DIRS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    public OliveTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
            return false;
        }
        Map<BlockPos, BlockState> logs = new HashMap<>();
        Set<BlockPos> leaves = new LinkedHashSet<>();
        BlockState log = GreekAgriculture.OLIVE_LOG.get().defaultBlockState();
        // 侧枝用四面树皮的橄榄木，避免横放原木露出年轮端面
        BlockState branch = GreekAgriculture.OLIVE_WOOD.get().defaultBlockState();

        // 主干：2–3 格，最上一格可能向一侧偏 1 格
        int trunk = 2 + random.nextInt(2);
        int[] lean = DIRS[random.nextInt(4) * 2];
        BlockPos top = origin;
        for (int i = 0; i < trunk; i++) {
            if (i == trunk - 1 && random.nextBoolean()) {
                top = top.offset(lean[0], 0, lean[1]);
            }
            logs.put(top, log);
            top = top.above();
        }
        // 侧枝：2–3 根，方向彼此至少相隔 90°（8 方向中相隔 ≥2）
        int branches = 2 + random.nextInt(2);
        List<Integer> used = new ArrayList<>();
        int start = random.nextInt(8);
        for (int b = 0; b < 8 && used.size() < branches; b++) {
            int d = (start + b * 3) % 8;
            if (used.stream().allMatch(u -> Math.min(Math.abs(u - d), 8 - Math.abs(u - d)) >= 2)) {
                used.add(d);
            }
        }
        for (int d : used) {
            int[] dir = DIRS[d];
            int len = 2 + random.nextInt(2);
            BlockPos p = top.below();
            for (int i = 1; i <= len; i++) {
                p = p.offset(dir[0], i % 2 == 0 ? 1 : 0, dir[1]);
                logs.put(p, branch);
            }
            canopy(p.above(), leaves, random);
        }
        canopy(top, leaves, random);
        leaves.removeAll(logs.keySet());

        for (BlockPos p : logs.keySet()) {
            if (!replaceable(level, p)) {
                return false;
            }
        }
        logs.forEach((p, s) -> level.setBlock(p, s, 19));
        BlockState leaf = GreekAgriculture.OLIVE_LEAVES.get().defaultBlockState();
        Map<BlockPos, Integer> distance = distances(logs.keySet(), leaves);
        for (BlockPos p : leaves) {
            if (replaceable(level, p) && distance.containsKey(p)) {
                level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, distance.get(p)), 19);
            }
        }
        return true;
    }

    /** 扁平树冠：中心层半径 2，上层半径 1，角上随机缺口。 */
    private static void canopy(BlockPos center, Set<BlockPos> out, RandomSource random) {
        for (int dy = 0; dy <= 1; dy++) {
            int r = dy == 0 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (corner && (r == 2 || random.nextBoolean())) {
                        continue;
                    }
                    out.add(center.offset(dx, dy, dz));
                }
            }
        }
    }

    private static boolean replaceable(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.SAPLINGS);
    }

    /** 树叶到最近原木的距离（原版树叶 1–7，超过 6 会腐烂；这里只保留 ≤6 的）。 */
    private static Map<BlockPos, Integer> distances(Set<BlockPos> logs, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos l : logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = l.relative(d);
                if (leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, 1);
                    queue.add(n);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int dp = dist.get(p);
            if (dp >= 6) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, dp + 1);
                    queue.add(n);
                }
            }
        }
        return dist;
    }
}
