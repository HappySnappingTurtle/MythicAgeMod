package io.github.happysnappingturtle.mythicage.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 柏树、月桂的树形（T13b 阶段 ③ 第 4 批）。规则同橄榄树：只替换空气与可替换方块，主干放不下时整棵不生成；
 * 世界生成与树苗共用。
 */
public final class GreekTreeShapes {
    private GreekTreeShapes() {
    }

    /**
     * 柏树（地中海柏树柱形 'Stricta'）：单干高 9–13，下面 2 格露干；树冠是贴着主干的细绿柱——
     * 下部六成 3×3（角上随机缺），往上收成十字，顶端单叶收尖。
     */
    public static final class Cypress extends Feature<NoneFeatureConfiguration> {
        private final Supplier<? extends net.minecraft.world.level.block.Block> log;
        private final Supplier<? extends net.minecraft.world.level.block.Block> leaves;

        public Cypress(Supplier<? extends net.minecraft.world.level.block.Block> log,
                       Supplier<? extends net.minecraft.world.level.block.Block> leaves) {
            super(NoneFeatureConfiguration.CODEC);
            this.log = log;
            this.leaves = leaves;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource random = ctx.random();
            BlockPos origin = ctx.origin();
            if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
                return false;
            }
            int height = 9 + random.nextInt(5);
            Set<BlockPos> logs = new LinkedHashSet<>();
            for (int y = 0; y <= height - 2; y++) {
                logs.add(origin.above(y));
            }
            Set<BlockPos> crown = new LinkedHashSet<>();
            for (int y = 2; y <= height; y++) {
                double t = (y - 2) / (double) (height - 2);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        boolean corner = dx != 0 && dz != 0;
                        boolean edge = !corner && (dx != 0 || dz != 0);
                        boolean keep = t < 0.6 ? !corner || random.nextInt(3) > 0
                                : t < 0.88 ? !corner && (!edge || t < 0.8 || random.nextBoolean())
                                : dx == 0 && dz == 0;
                        if (keep) {
                            crown.add(origin.offset(dx, y, dz));
                        }
                    }
                }
            }
            return build(level, logs, crown, log.get().defaultBlockState(), leaves.get().defaultBlockState());
        }
    }

    /**
     * 月桂（常绿小乔木 / 大灌木）：主干 2–3 格，顶上一团浓密的圆冠（半径 2、高 3–4 层），
     * 冠底一层与冠顶收小，边角随机缺口；总高 5–7。
     */
    public static final class Laurel extends Feature<NoneFeatureConfiguration> {
        private final Supplier<? extends net.minecraft.world.level.block.Block> log;
        private final Supplier<? extends net.minecraft.world.level.block.Block> leaves;

        public Laurel(Supplier<? extends net.minecraft.world.level.block.Block> log,
                      Supplier<? extends net.minecraft.world.level.block.Block> leaves) {
            super(NoneFeatureConfiguration.CODEC);
            this.log = log;
            this.leaves = leaves;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource random = ctx.random();
            BlockPos origin = ctx.origin();
            if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
                return false;
            }
            int trunk = 2 + random.nextInt(2);
            int layers = 3 + random.nextInt(2);
            Set<BlockPos> logs = new LinkedHashSet<>();
            for (int y = 0; y < trunk + layers - 1; y++) {
                logs.add(origin.above(y));
            }
            Set<BlockPos> crown = new LinkedHashSet<>();
            for (int i = 0; i < layers; i++) {
                int y = trunk + i - 1;
                boolean small = i == 0 || i == layers - 1;
                int r = small ? 1 : 2;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                        if (corner && (r == 2 || random.nextInt(3) == 0)) {
                            continue;
                        }
                        // 外圈边缘随机缺口，冠形不至于方正
                        if (r == 2 && (Math.abs(dx) == 2 || Math.abs(dz) == 2) && random.nextInt(5) == 0) {
                            continue;
                        }
                        crown.add(origin.offset(dx, y, dz));
                    }
                }
            }
            crown.add(origin.above(trunk + layers - 1));
            return build(level, logs, crown, log.get().defaultBlockState(), leaves.get().defaultBlockState());
        }
    }

    private static boolean build(WorldGenLevel level, Set<BlockPos> logs, Set<BlockPos> crown, BlockState log,
                                 BlockState leaf) {
        crown.removeAll(logs);
        for (BlockPos p : logs) {
            if (!OliveTreeFeature.replaceable(level, p)) {
                return false;
            }
        }
        logs.forEach(p -> level.setBlock(p, log, 19));
        Map<BlockPos, Integer> distance = OliveTreeFeature.distances(logs, crown);
        for (BlockPos p : crown) {
            if (OliveTreeFeature.replaceable(level, p) && distance.containsKey(p)) {
                level.setBlock(p, leaf.setValue(LeavesBlock.DISTANCE, distance.get(p)), 19);
            }
        }
        return true;
    }
}
