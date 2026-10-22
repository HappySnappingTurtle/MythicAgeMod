package io.github.happysnappingturtle.mythicage.wild.greek;

import com.mojang.serialization.Codec;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.wild.WildCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.UUID;

/**
 * 半人马营地（W1，T15 设计文档 §2）：程序化小地物。踩实的空地（砂土）中央一堆点着的营火，两根横放的去皮原木当座位，
 * 外圈三个草垛箭靶（草块 + 标靶），一侧一顶皮帐（四根栅栏撑起棕色羊毛顶），旁边一两个草捆；生成时放入 3–5 只半人马（含 1 只首领）。
 * 地面起伏超过 2 格、或中心不是草 / 土（水面、沙地）就不生成；高度按 MOTION_BLOCKING_NO_LEAVES 取（不算草丛、花与树叶）。
 */
public class CentaurCampFeature extends Feature<NoneFeatureConfiguration> {
    public static final int RADIUS = 6;

    public CentaurCampFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX(), o.getZ());
        int lo = y0;
        int hi = y0;
        for (int dx = -RADIUS; dx <= RADIUS; dx += RADIUS) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += RADIUS) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX() + dx, o.getZ() + dz);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        BlockPos center = new BlockPos(o.getX(), y0, o.getZ());
        BlockState ground = level.getBlockState(center.below());
        if (hi - lo > 2 || !(ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT))) {
            return false;
        }
        build(level, center, r);
        spawnGroup(level, center, r);
        return true;
    }

    /**
     * 方块部分（GameTest 也直接调用）。center 是营火所在格（地面上一格）。地面高度在 center 上下几格内就地查找，
     * 不用高度图（生成完成后 *_WG 高度图就没了；GameTest 场地的高度图还会算上外框）。
     */
    public static void build(WorldGenLevel level, BlockPos center, RandomSource r) {
        // 清掉空地上的草与花，地面踩成砂土
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                BlockPos top = ground(level, center.offset(dx, 0, dz));
                for (int up = 0; up < 3; up++) {
                    BlockPos p = top.above(up - 1);
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.FLOWERS)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                if (dx * dx + dz * dz <= 9 && r.nextFloat() < 0.75F) {
                    BlockPos g = ground(level, center.offset(dx, 0, dz)).below();
                    if (level.getBlockState(g).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(g, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                    }
                }
            }
        }
        level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        // 两根横放的去皮橡木当座位
        BlockState seatX = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState seatZ = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        place(level, center.offset(-1, 0, 2), seatX);
        place(level, center.offset(0, 0, 2), seatX);
        place(level, center.offset(-2, 0, -1), seatZ);
        place(level, center.offset(-2, 0, 0), seatZ);
        // 外圈三个草垛箭靶，错开东北边的皮帐（帐子在约 -37° 方向，靶从它两侧 60° 起每 120° 一个）
        double base = Math.atan2(-3, 4) + Math.PI / 3 + (r.nextDouble() - 0.5) * 0.3;
        for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2 / 3;
            BlockPos t = new BlockPos(center.getX() + (int) Math.round(Math.cos(a) * 5), center.getY(),
                    center.getZ() + (int) Math.round(Math.sin(a) * 5));
            BlockPos ground = ground(level, t);
            place(level, ground, Blocks.HAY_BLOCK.defaultBlockState());
            place(level, ground.above(), Blocks.TARGET.defaultBlockState());
        }
        // 皮帐：营火东边的小人字帐，两侧棕色羊毛、顶上一道脊，前面敞开铺毯子，两端露出深色橡木帐杆
        BlockPos tent = ground(level, center.offset(3, 0, -3));
        BlockState hide = Blocks.BROWN_WOOL.defaultBlockState();
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos below = tent.offset(dx, -1, dz);
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    place(level, below, Blocks.DIRT.defaultBlockState());
                }
                for (int up = 0; up < 3; up++) {
                    place(level, tent.offset(dx, up, dz), Blocks.AIR.defaultBlockState());
                }
            }
            place(level, tent.offset(dx, 0, -1), hide);
            place(level, tent.offset(dx, 0, 1), hide);
            place(level, tent.offset(dx, 1, 0), hide);
        }
        place(level, tent.offset(2, 0, 0), hide);
        place(level, tent.offset(0, 0, 0), Blocks.BROWN_CARPET.defaultBlockState());
        place(level, tent.offset(1, 0, 0), Blocks.BROWN_CARPET.defaultBlockState());
        place(level, tent.offset(0, 2, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
        place(level, tent.offset(2, 2, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
        // 草捆
        place(level, ground(level, center.offset(-3, 0, -3)), Blocks.HAY_BLOCK.defaultBlockState());
    }

    /** 以 ref 的高度为准，在上下 4 格内找地面：返回脚下是实心方块、本身可替换的那一格。 */
    static BlockPos ground(WorldGenLevel level, BlockPos ref) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos p = ref.above(dy);
            BlockState here = level.getBlockState(p);
            BlockState below = level.getBlockState(p.below());
            if ((here.isAir() || here.canBeReplaced()) && below.isFaceSturdy(level, p.below(), Direction.UP)) {
                return p;
            }
        }
        return ref;
    }

    private static void place(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, 2);
    }

    /** 放入 3–5 只（按生物卡的群大小），第一只是首领。 */
    public static int spawnGroup(WorldGenLevel level, BlockPos center, RandomSource r) {
        WildCreatureCard card = WildCreatures.card("greek:centaur").orElse(null);
        int min = card != null ? card.minGroup() : 3;
        int max = card != null ? card.maxGroup() : 5;
        int n = min + r.nextInt(max - min + 1);
        UUID group = UUID.randomUUID();
        for (int i = 0; i < n; i++) {
            WildCentaurEntity c = GreekWild.CENTAUR.get().create(level.getLevel());
            if (c == null) {
                return i;
            }
            double a = r.nextDouble() * Math.PI * 2;
            double d = 3.0 + r.nextDouble() * 1.5;
            BlockPos at = ground(level, center.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d)));
            c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            c.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            c.joinGroup(group, center, i == 0);
            level.addFreshEntityWithPassengers(c);
        }
        return n;
    }
}
