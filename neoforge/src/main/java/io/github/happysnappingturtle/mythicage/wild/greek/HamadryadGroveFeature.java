package io.github.happysnappingturtle.mythicage.wild.greek;

import com.mojang.serialization.Codec;
import io.github.happysnappingturtle.mythicage.entity.hamadryad.HamadryadEntity;
import io.github.happysnappingturtle.mythicage.wild.WildCreatures;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.wild.WildSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 树仙女圣林（T15 §6、制作卡 10 §1）：中间一棵本命橡木（专用深色原木），周围三棵普通橡树；每棵树叶下挂几条还愿彩带
 * （奥维德《变形记》8.744），一棵橡树干上有蜂巢（01 号：与蜜蜂共生）；本命树 3 格内清掉杂草（01 号迹象：本命树旁不长杂草）。
 * 生成时放 1–2 只树仙女绑定本命树。只在繁花森林、草甸；圣林间距按生物卡的区域密度（320 格见方 1 处）。
 */
public class HamadryadGroveFeature extends Feature<NoneFeatureConfiguration> {
    private static final String KEY = "greek:hamadryad";
    private static final int[][] OAKS = {{6, 1}, {-5, 4}, {1, -6}};

    public HamadryadGroveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos o = ctx.origin();
        if (!WildSpawning.areaSiteAllows(level, o, KEY, 320)) {
            return false;
        }
        int y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX(), o.getZ());
        BlockPos center = new BlockPos(o.getX(), y0, o.getZ());
        if (!level.getBlockState(center.below()).is(BlockTags.DIRT)) {
            return false;
        }
        for (int[] d : OAKS) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX() + d[0], o.getZ() + d[1]);
            if (Math.abs(y - y0) > 2) {
                return false;
            }
        }
        BlockPos root = build(level, center, ctx.random());
        spawn(level, root, ctx.random());
        return true;
    }

    /** 方块部分。center 是本命树树根那一格（地面上一格）。返回树根坐标。 */
    public static BlockPos build(WorldGenLevel level, BlockPos center, RandomSource r) {
        BlockState bound = GreekWild.HAMADRYAD_OAK_LOG.get().defaultBlockState();
        // 本命树 3 格内清掉草与花
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 1, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.FLOWERS)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        oak(level, center, bound, 7, r);
        for (int i = 0; i < OAKS.length; i++) {
            int[] d = OAKS[i];
            BlockPos at = ground(level, center.offset(d[0], 0, d[1]));
            oak(level, at, Blocks.OAK_LOG.defaultBlockState(), 5 + r.nextInt(2), r);
            if (i == 0) {
                BlockPos hive = at.above(2).relative(Direction.WEST);
                level.setBlock(hive, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, Direction.WEST), 2);
                if (level.getBlockEntity(hive) instanceof BeehiveBlockEntity be) {
                    for (int b = 0; b < 2; b++) {
                        be.storeBee(BeehiveBlockEntity.Occupant.create(r.nextInt(599)));
                    }
                }
            }
        }
        return center.immutable();
    }

    private static BlockPos ground(WorldGenLevel level, BlockPos near) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos p = near.above(dy);
            if (level.getBlockState(p.below()).is(BlockTags.DIRT) && !level.getBlockState(p).isSolid()) {
                return p;
            }
        }
        return near;
    }

    /** 一棵简单的橡树：树干 height 格、两根侧枝、叶团（半径 3 的扁球）；叶团下挂 2–3 条彩带。 */
    private static void oak(WorldGenLevel level, BlockPos base, BlockState log, int height, RandomSource r) {
        for (int i = 0; i < height; i++) {
            level.setBlock(base.above(i), log, 2);
        }
        BlockPos top = base.above(height - 1);
        BlockState sideX = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState sideZ = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        level.setBlock(base.above(height - 3).east(), sideX, 2);
        level.setBlock(base.above(height - 2).north(), sideZ, 2);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double d = dx * dx / 9.0 + dy * dy / 4.0 + dz * dz / 9.0;
                    if (d > 1.0 || (d > 0.75 && r.nextFloat() < 0.4)) {
                        continue;
                    }
                    BlockPos p = top.offset(dx, dy + 1, dz);
                    if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, leaves, 2);
                    }
                }
            }
        }
        int ribbons = 2 + r.nextInt(2);
        for (int tries = 0; ribbons > 0 && tries < 30; tries++) {
            BlockPos p = top.offset(r.nextInt(5) - 2, -1, r.nextInt(5) - 2);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).is(BlockTags.LEAVES)) {
                level.setBlock(p, GreekWild.VOTIVE_RIBBON.get().defaultBlockState(), 2);
                ribbons--;
            }
        }
    }

    /** 放 1–2 只树仙女绑定本命树。返回只数。 */
    public static int spawn(WorldGenLevel level, BlockPos root, RandomSource r) {
        WildCreatureCard card = WildCreatures.card(KEY).orElse(null);
        int min = card != null ? card.minGroup() : 1;
        int max = card != null ? card.maxGroup() : 2;
        int n = min + r.nextInt(max - min + 1);
        for (int i = 0; i < n; i++) {
            HamadryadEntity h = GreekWild.HAMADRYAD.get().create(level.getLevel());
            if (h == null) {
                return i;
            }
            h.moveTo(root.getX() + 0.5 + (i == 0 ? 0 : 1.5), root.getY(), root.getZ() + 2.5, r.nextFloat() * 360, 0);
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(root), MobSpawnType.STRUCTURE, null);
            h.bindTree(root);
            level.addFreshEntityWithPassengers(h);
        }
        return n;
    }
}
