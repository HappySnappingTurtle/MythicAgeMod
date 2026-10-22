package io.github.happysnappingturtle.mythicage.wild.greek;

import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import io.github.happysnappingturtle.mythicage.wild.WildCreatures;
import io.github.happysnappingturtle.mythicage.wild.WildSpawning;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import java.util.UUID;

/**
 * 斯廷法利斯鸟巢（T15 §3、制作卡 07 §1）：沼泽里一棵枯树——深色橡木树干从泥里或浅水里长出，根部一圈红树根，
 * 中段和上段伸出几根光秃的枝，树顶是 3×3 的草巢（中间干草、四周红树根）；树下泥地插着几根青铜羽。
 * 巢间距按生物卡的区域密度（384 格见方最多 1 巢），由 {@link WildSpawning#areaSiteAllows} 挑区块。
 * 生成时把 5–8 只怪鸟（按生物卡的群大小）放在巢上，第一只是领头鸟。程序化地物（D8），GameTest 直接调用 {@link #build}。
 */
public class StymphalianNestFeature extends Feature<NoneFeatureConfiguration> {
    public static final int TRUNK = 7;
    private static final String KEY = "greek:stymphalian_bird";

    public StymphalianNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!WildSpawning.areaSiteAllows(level, origin, KEY, 384)) {
            return false;
        }
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        BlockPos base = new BlockPos(origin.getX(), y, origin.getZ());
        // 树根处的水不能超过 2 格深；脚下得是泥、土或草
        BlockState ground = level.getBlockState(base.below());
        if (!(ground.is(BlockTags.DIRT) || ground.is(Blocks.MUD) || ground.is(Blocks.CLAY))
                || !level.getFluidState(base.above(2)).isEmpty()) {
            return false;
        }
        if (!WildSpawning.villageAllowsDuringGen(level.getLevel(), base, WildCreatures.card(KEY).orElse(null))) {
            return false;
        }
        BlockPos nest = build(level, base, ctx.random());
        spawnFlock(level, nest, ctx.random());
        return true;
    }

    /**
     * 方块部分。base 是树干最下一格（地面上一格，可以在浅水里）。返回巢上落脚处（草巢上面那一格的空气），
     * 怪鸟的 nest 坐标就是它。
     */
    public static BlockPos build(WorldGenLevel level, BlockPos base, RandomSource r) {
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState logX = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        // 树下一圈泥（草、土换成泥；水下的也换）
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 16 || r.nextFloat() < 0.2F) {
                    continue;
                }
                BlockPos g = base.offset(dx, -1, dz);
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos p = g.above(dy);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)) {
                        if (level.getBlockState(p.above()).getFluidState().isEmpty() || dy <= 0) {
                            level.setBlock(p, Blocks.MUD.defaultBlockState(), 2);
                        }
                        break;
                    }
                }
            }
        }
        // 树干、根部红树根
        for (int i = 0; i < TRUNK; i++) {
            level.setBlock(base.above(i), log, 2);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos root = base.relative(d);
            BlockState here = level.getBlockState(root);
            if (here.isAir() || here.canBeReplaced()) {
                level.setBlock(root, Blocks.MANGROVE_ROOTS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,
                                level.getFluidState(root).is(Fluids.WATER)), 2);
            }
        }
        // 枯枝：中段两根相对、上段一根，枝梢朝上翘一截栅栏
        branch(level, base.above(3), Direction.EAST, logX, 2);
        branch(level, base.above(4), Direction.WEST, logX, 2);
        branch(level, base.above(5), Direction.NORTH, logZ, 2);
        // 树顶草巢：中间干草，四周红树根
        BlockPos top = base.above(TRUNK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(top.offset(dx, 0, dz), dx == 0 && dz == 0 ? Blocks.HAY_BLOCK.defaultBlockState()
                        : Blocks.MANGROVE_ROOTS.defaultBlockState(), 2);
                for (int up = 1; up <= 2; up++) {
                    level.setBlock(top.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // 泥地上插几根青铜羽（脚下是实心方块、本身是空气才插）
        int feathers = 4 + r.nextInt(3);
        for (int i = 0, tries = 0; i < feathers && tries < 40; tries++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 2 + r.nextDouble() * 3;
            BlockPos ref = base.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d));
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = ref.above(dy);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    level.setBlock(p, GreekWild.STUCK_BRONZE_FEATHER.get().defaultBlockState(), 2);
                    i++;
                    break;
                }
            }
        }
        return top.above();
    }

    private static void branch(WorldGenLevel level, BlockPos from, Direction d, BlockState log, int len) {
        BlockPos p = from;
        for (int i = 0; i < len; i++) {
            p = p.relative(d);
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                level.setBlock(p, log, 2);
            }
        }
        BlockPos tip = p.above();
        if (level.getBlockState(tip).isAir()) {
            level.setBlock(tip, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
        }
    }

    /** 放入 5–8 只（按生物卡的群大小）落在巢上，第一只是领头鸟。返回只数。 */
    public static int spawnFlock(WorldGenLevel level, BlockPos nest, RandomSource r) {
        WildCreatureCard card = WildCreatures.card(KEY).orElse(null);
        int min = card != null ? card.minGroup() : 5;
        int max = card != null ? card.maxGroup() : 8;
        int n = min + r.nextInt(max - min + 1);
        UUID group = UUID.randomUUID();
        for (int i = 0; i < n; i++) {
            StymphalianBirdEntity b = GreekWild.STYMPHALIAN_BIRD.get().create(level.getLevel());
            if (b == null) {
                return i;
            }
            b.moveTo(nest.getX() + 0.5 + (i % 3) - 1, nest.getY() + 0.1, nest.getZ() + 0.5 + (i / 3 % 3) - 1, r.nextFloat() * 360, 0);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(nest), MobSpawnType.STRUCTURE, null);
            b.joinFlock(group, nest, i == 0);
            level.addFreshEntityWithPassengers(b);
        }
        return n;
    }
}
