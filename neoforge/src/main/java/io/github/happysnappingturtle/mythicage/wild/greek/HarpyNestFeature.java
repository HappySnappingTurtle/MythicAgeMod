package io.github.happysnappingturtle.mythicage.wild.greek;

import com.mojang.serialization.Codec;
import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.entity.harpy.HarpyEntity;
import io.github.happysnappingturtle.mythicage.wild.WildCreatures;
import io.github.happysnappingturtle.mythicage.wild.WildSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.UUID;

/**
 * 哈耳庇厄崖巢（T15 §4、制作卡 08 §1）：风暴高地或爱琴海岸的崖顶边缘。3 × 3 的粗土巢台，角上一截骨块、
 * 一个骷髅头和枯灌木；崖面上从巢台往下流几道方解石白痕（维吉尔 3.216 写她们留下的污秽，阿波罗尼奥斯 2.191 的恶臭）。
 * 生成时放入 2–4 只哈耳庇厄落在巢上。程序化地物（D8），GameTest 直接调用 {@link #build}。
 * <p>
 * 找崖：在落点周围 ±7 格内每 2 格取一列，地面要是石头、泥土、砂砾类，且某个方向 2–3 格外的地面低 ≥ 6 格。
 * 巢间距按生物卡的区域密度（256 格见方最多 2 处）。
 */
public class HarpyNestFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_DROP = 6;
    private static final String KEY = "greek:harpy";

    public HarpyNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!WildSpawning.areaSiteAllows(level, origin, KEY, 256)) {
            return false;
        }
        Cliff c = findCliff(level, origin, 7, 2);
        if (c == null || !WildSpawning.villageAllowsDuringGen(level.getLevel(), c.top(), WildCreatures.card(KEY).orElse(null))) {
            return false;
        }
        BlockPos nest = build(level, c.top(), c.drop(), ctx.random());
        spawnFlock(level, nest, ctx.random());
        return true;
    }

    /** 崖顶地面上一格与落差方向。 */
    public record Cliff(BlockPos top, Direction drop) {
    }

    /**
     * 在 origin 周围 ±radius 格内每 step 格取一列找崖边：地面是石头、泥土、砂砾类，某个方向 2–3 格外的地面低 ≥ 6 格，
     * 背后那格与它高差 ≤ 1，且以它为中心的 3 × 3 至少 7 格地面同高（够放巢台、骨堆与落脚处）。找不到返回 null。
     */
    @javax.annotation.Nullable
    public static Cliff findCliff(WorldGenLevel level, BlockPos origin, int radius, int step) {
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = height(level, x, z);
                BlockPos at = new BlockPos(x, top, z);
                if (!groundOk(level.getBlockState(at.below()))) {
                    continue;
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int low = Math.max(height(level, x + d.getStepX() * 2, z + d.getStepZ() * 2),
                            height(level, x + d.getStepX() * 3, z + d.getStepZ() * 3));
                    int back = height(level, x - d.getStepX(), z - d.getStepZ());
                    if (top - low >= MIN_DROP && Math.abs(back - top) <= 1 && flatLedge(level, x, z, top)) {
                        return new Cliff(at, d);
                    }
                }
            }
        }
        return null;
    }

    private static boolean flatLedge(WorldGenLevel level, int x, int z, int top) {
        int flat = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                flat += height(level, x + i, z + j) == top ? 1 : 0;
            }
        }
        return flat >= 7;
    }

    private static int height(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean groundOk(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.STONE)
                || s.is(Blocks.CALCITE) || s.is(Blocks.TUFF);
    }

    /** 能刷上白痕的崖面方块（不动矿石以外的贵重方块）。 */
    private static boolean stainable(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.STONE);
    }

    /**
     * 方块部分。base 是崖顶地面上一格（巢台中心的空气），drop 是落差方向。返回巢上落脚处（= base）。
     */
    public static BlockPos build(WorldGenLevel level, BlockPos base, Direction drop, RandomSource r) {
        // 巢台：3 × 3 粗土（只换地表那一格），上面清空
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos g = base.offset(dx, -1, dz);
                if (level.getBlockState(g).isSolid()) {
                    level.setBlock(g, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
                for (int up = 0; up <= 2; up++) {
                    BlockPos a = base.offset(dx, up, dz);
                    if (!level.getBlockState(a).isAir() && level.getBlockState(a).canBeReplaced()) {
                        level.setBlock(a, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // 骨堆：一截横放的骨块、一个骷髅头、一丛枯灌木，按「背离崖边的角 → 两侧 → 靠崖边」的顺序放在巢台上空着的格子里
        Direction side = drop.getClockWise();
        Direction in = drop.getOpposite();
        BlockPos[] spots = {base.relative(in).relative(side), base.relative(in).relative(side.getOpposite()), base.relative(in),
                base.relative(side), base.relative(side.getOpposite()), base.relative(drop).relative(side),
                base.relative(drop).relative(side.getOpposite())};
        BlockState[] pile = {Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, side.getAxis()),
                Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r.nextInt(16)),
                Blocks.DEAD_BUSH.defaultBlockState()};
        int next = 0;
        for (BlockPos p : spots) {
            if (next < pile.length && placeIfAir(level, p, pile[next])) {
                next++;
            }
        }
        // 白痕：沿崖边三列，往落差方向找到崖面那一列，从地表下一格起往下刷 2–5 格方解石（地表那格是巢台，不动）
        for (int k = -1; k <= 1; k++) {
            BlockPos col = base.relative(side, k);
            for (int out = 1; out <= 3; out++) {
                BlockPos face = col.relative(drop, out);
                if (!level.getBlockState(face.below()).isSolid()) {
                    BlockPos wall = col.relative(drop, out - 1);
                    int len = 2 + r.nextInt(4);
                    for (int dy = 2; dy <= len + 1; dy++) {
                        BlockPos w = wall.below(dy);
                        if (stainable(level.getBlockState(w))) {
                            level.setBlock(w, Blocks.CALCITE.defaultBlockState(), 2);
                        }
                    }
                    break;
                }
            }
        }
        return base.immutable();
    }

    private static boolean placeIfAir(WorldGenLevel level, BlockPos p, BlockState s) {
        if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolid()) {
            level.setBlock(p, s, 2);
            return true;
        }
        return false;
    }

    /** 放入 2–4 只（按生物卡的群大小）落在巢上。返回只数。 */
    public static int spawnFlock(WorldGenLevel level, BlockPos nest, RandomSource r) {
        WildCreatureCard card = WildCreatures.card(KEY).orElse(null);
        int min = card != null ? card.minGroup() : 2;
        int max = card != null ? card.maxGroup() : 4;
        int n = min + r.nextInt(max - min + 1);
        UUID group = UUID.randomUUID();
        for (int i = 0; i < n; i++) {
            HarpyEntity h = GreekWild.HARPY.get().create(level.getLevel());
            if (h == null) {
                return i;
            }
            h.moveTo(nest.getX() + 0.5 + (i % 2), nest.getY() + 0.1, nest.getZ() + 0.5 + (i / 2) - 0.5, r.nextFloat() * 360, 0);
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(nest), MobSpawnType.STRUCTURE, null);
            h.joinFlock(group, nest);
            level.addFreshEntityWithPassengers(h);
        }
        return n;
    }
}
