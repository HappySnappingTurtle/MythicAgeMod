package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuiltArea;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 村外的天然资源（T19 设计文档 §6.1，D5、D6）：
 * <ul>
 *   <li><b>登记林地</b>：村外一定半径内的天然树木（树干底下是土、树冠是非玩家放置的树叶），最多若干株，每株一个
 *       {@code woodland} 资源点；伐倒后余量为 0，原地长回树干时恢复；</li>
 *   <li><b>天然资源点</b>（如远古采石点）：先找露出地面的指定方块；找不到且允许时，在资源区一块平整、无人动过的地上
 *       放一处 3×3×2 的露头。</li>
 * </ul>
 * 都只看<b>已加载</b>的格子：没加载的地方等加载后下一次再补，不把未加载格当空地。不碰已建范围（玩家的田和房子也在外面
 * 时，非自然树叶、非地表方块都会被排除）。
 */
public final class NaturalResources {
    public static final String WOODLAND = "woodland";
    /** 林地两株之间至少隔这么远，避免一棵大树登记成好几株。 */
    public static final int TREE_SPACING = 4;
    /** 自己放的露头的资源点 id 后缀；只有它会随余量改外观。 */
    public static final String OUTCROP = "_outcrop";

    private NaturalResources() {
    }

    // ───────────── 林地 ─────────────

    /** 补登记林地（每 MC 日一次）。返回新登记的株数。 */
    public static int registerWoodland(VillageContext ctx) {
        CivVillageData.Woodland w = ctx.data().woodland();
        if (w == null) {
            return 0;
        }
        List<ResourcePoint> have = new ArrayList<>(ctx.state().points.values().stream()
                .filter(p -> p.kind().equals(WOODLAND)).toList());
        if (have.size() >= w.max()) {
            return 0;
        }
        ServerLevel level = ctx.level();
        BlockPos c = ctx.center();
        BuiltArea built = ctx.plan() == null ? null : ctx.plan().area();
        int added = 0;
        int next = have.size();
        for (int dx = -w.radius(); dx <= w.radius() && have.size() < w.max(); dx += 2) {
            for (int dz = -w.radius(); dz <= w.radius() && have.size() < w.max(); dz += 2) {
                int x = c.getX() + dx;
                int z = c.getZ() + dz;
                if (built != null && built.within(x, z, 2)) {
                    continue;
                }
                if (!level.isLoaded(new BlockPos(x, c.getY(), z))) {
                    continue;
                }
                Optional<BlockPos> base = treeBase(level, x, z);
                if (base.isEmpty()) {
                    continue;
                }
                BlockPos b = base.get();
                if (have.stream().anyMatch(p -> Math.abs(p.pos().x() - b.getX()) < TREE_SPACING
                        && Math.abs(p.pos().z() - b.getZ()) < TREE_SPACING)) {
                    continue;
                }
                String id = WOODLAND + "_" + next++;
                ResourcePoint rp = new ResourcePoint(id, WOODLAND, VillageContext.pos(b), 1, 1, 0, ctx.today());
                ctx.state().points.put(id, rp);
                have.add(rp);
                added++;
            }
        }
        if (added > 0) {
            MythicAge.LOGGER.debug("村庄 {} 登记林地 {} 株（共 {}）", ctx.village().id(), added, have.size());
        }
        return added;
    }

    /** (x, z) 这一列有没有一棵天然树：树干最高处往下找到底，底下是土；树干顶上附近有非玩家放置的树叶。 */
    /** 这一列最高的地面方块：高度图可能偏高，往下走过空气与屏障（GameTest 场地四周罩着屏障）；最多 24 格。 */
    static int surfaceY(ServerLevel level, int x, int z, Heightmap.Types type) {
        int y = level.getHeight(type, x, z) - 1;
        int guard = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
        while (guard++ < 24 && y > level.getMinBuildHeight()) {
            BlockState s = level.getBlockState(m.setY(y));
            if (!s.isAir() && !s.is(Blocks.BARRIER) && !(type == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES && s.getBlock() instanceof LeavesBlock)) {
                break;
            }
            y--;
        }
        return y;
    }

    static Optional<BlockPos> treeBase(ServerLevel level, int x, int z) {
        int top = surfaceY(level, x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        BlockPos p = new BlockPos(x, top, z);
        if (!level.getBlockState(p).is(BlockTags.LOGS)) {
            return Optional.empty();
        }
        BlockPos topLog = p;
        int guard = 0;
        while (level.getBlockState(p.below()).is(BlockTags.LOGS) && guard++ < 24) {
            p = p.below();
        }
        if (!level.getBlockState(p.below()).is(BlockTags.DIRT)) {
            return Optional.empty();
        }
        return naturalLeavesNear(level, topLog) ? Optional.of(p.immutable()) : Optional.empty();
    }

    static boolean naturalLeavesNear(ServerLevel level, BlockPos topLog) {
        for (BlockPos q : BlockPos.betweenClosed(topLog.offset(-3, -2, -3), topLog.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(q);
            if (s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }

    /** 伐倒的树长回来了（树干又在原地）：余量恢复。 */
    public static void refreshWoodland(VillageContext ctx) {
        ctx.state().points.replaceAll((id, p) -> {
            if (!p.kind().equals(WOODLAND) || p.remaining() > 0) {
                return p;
            }
            BlockPos b = VillageContext.block(p.pos());
            return ctx.loaded(b) && ctx.level().getBlockState(b).is(BlockTags.LOGS)
                    ? new ResourcePoint(p.id(), p.kind(), p.pos(), 1, 1, 0, ctx.today()) : p;
        });
    }

    /** 伐一株：从树干底往上把相连的同种原木都砍下（最多 64 块，水平 5 格内），补种树苗。返回物品 id → 数量。 */
    public static java.util.Map<String, Integer> fell(ServerLevel level, BlockPos base) {
        BlockState baseState = level.getBlockState(base);
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (!baseState.is(BlockTags.LOGS)) {
            return out;
        }
        Block log = baseState.getBlock();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        queue.add(base);
        seen.add(base.asLong());
        int n = 0;
        while (!queue.isEmpty() && n < 64) {
            BlockPos p = queue.poll();
            if (!level.getBlockState(p).is(log)) {
                continue;
            }
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            n++;
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                if (Math.abs(q.getX() - base.getX()) <= 5 && Math.abs(q.getZ() - base.getZ()) <= 5 && q.getY() >= base.getY()
                        && seen.add(q.asLong())) {
                    queue.add(q.immutable());
                }
            }
        }
        out.put(BuiltInRegistries.ITEM.getKey(log.asItem()).toString(), n);
        sapling(log).ifPresent(sap -> {
            if (level.getBlockState(base.below()).is(BlockTags.DIRT) && level.getBlockState(base).isAir()) {
                level.setBlock(base, sap.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        return out;
    }

    /** 原木对应的树苗：{@code xxx_log} → {@code xxx_sapling}（找不到就不补种）。 */
    static Optional<Block> sapling(Block log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log);
        String path = id.getPath();
        if (!path.endsWith("_log")) {
            return Optional.empty();
        }
        ResourceLocation sap = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - 4) + "_sapling");
        return BuiltInRegistries.BLOCK.getOptional(sap);
    }

    // ───────────── 天然资源点（采石点）─────────────

    /** 还没有这种资源点时找一处（D6）。返回是否新登记了。 */
    public static boolean registerNatural(VillageContext ctx, CivVillageData.NaturalPoint np) {
        if (ctx.state().points.values().stream().anyMatch(p -> p.kind().equals(np.kind()))) {
            return false;
        }
        CivVillageData.PointKind k = ctx.data().points().get(np.kind());
        if (k == null) {
            return false;
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(np.block()));
        if (block.isEmpty()) {
            return false;
        }
        Optional<BlockPos> at = findSurface(ctx, block.get(), np.radius());
        String id = np.kind() + "_0";
        if (at.isEmpty() && np.outcrop()) {
            at = placeOutcrop(ctx, block.get(), np.radius());
            id = np.kind() + OUTCROP;
        }
        if (at.isEmpty()) {
            return false;
        }
        ctx.state().points.put(id, ResourcePoint.fresh(id, np.kind(), VillageContext.pos(at.get()), k.max(), k.recover(), ctx.today()));
        ctx.state().log(ctx.today(), "mythic_age.village.log.point_found", "@mythic_age.village.point." + np.kind());
        return true;
    }

    /** 半径内露出地面的这种方块，取离村中心最近的。 */
    static Optional<BlockPos> findSurface(VillageContext ctx, Block block, int radius) {
        ServerLevel level = ctx.level();
        BlockPos c = ctx.center();
        BuiltArea built = ctx.plan() == null ? null : ctx.plan().area();
        BlockPos best = null;
        long bestD = Long.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = c.getX() + dx;
                int z = c.getZ() + dz;
                if ((built != null && built.within(x, z, 0)) || !level.isLoaded(new BlockPos(x, c.getY(), z))) {
                    continue;
                }
                BlockPos top = new BlockPos(x, surfaceY(level, x, z, Heightmap.Types.WORLD_SURFACE), z);
                if (level.getBlockState(top).is(block)) {
                    long d = (long) dx * dx + (long) dz * dz;
                    if (d < bestD) {
                        bestD = d;
                        best = top;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 在村外一块平整的地上放 3×3×2 的露头，返回顶层中心。找不到合适的地就不放。 */
    static Optional<BlockPos> placeOutcrop(VillageContext ctx, Block block, int radius) {
        ServerLevel level = ctx.level();
        BlockPos c = ctx.center();
        BuiltArea built = ctx.plan() == null ? null : ctx.plan().area();
        for (int dist = 14; dist <= radius; dist += 6) {
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                int x = c.getX() + (int) Math.round(Math.cos(a) * dist);
                int z = c.getZ() + (int) Math.round(Math.sin(a) * dist);
                if (built != null && built.within(x, z, 3)) {
                    continue;
                }
                Optional<Integer> y = flat3x3(level, x, z);
                if (y.isPresent()) {
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            level.setBlock(new BlockPos(x + ox, y.get(), z + oz), block.defaultBlockState(), Block.UPDATE_ALL);
                            level.setBlock(new BlockPos(x + ox, y.get() + 1, z + oz), block.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    return Optional.of(new BlockPos(x, y.get() + 1, z));
                }
            }
        }
        return Optional.empty();
    }

    /** 3×3 九列都已加载、地面同高、地面是土或石头、上面两格是空气（或可替换的草花）。返回地面上第一格的 y。 */
    static Optional<Integer> flat3x3(ServerLevel level, int x, int z) {
        Integer y0 = null;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                BlockPos col = new BlockPos(x + ox, 0, z + oz);
                if (!level.isLoaded(col)) {
                    return Optional.empty();
                }
                int y = surfaceY(level, x + ox, z + oz, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES) + 1;
                if (y0 == null) {
                    y0 = y;
                } else if (y != y0) {
                    return Optional.empty();
                }
                BlockState ground = level.getBlockState(new BlockPos(x + ox, y - 1, z + oz));
                if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.BASE_STONE_OVERWORLD))) {
                    return Optional.empty();
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockState above = level.getBlockState(new BlockPos(x + ox, y + dy, z + oz));
                    if (!above.isAir() && !above.canBeReplaced()) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.ofNullable(y0);
    }

    /**
     * 采石点露头的分档外观：满 → 3×3×2；半 → 上层只剩中间一个十字；少 → 上层采空；空 → 下层换成圆石与砂砾。
     * 只改还是这种石头的格子（玩家改过的不动）。
     */
    public static void quarryVisual(VillageContext ctx, ResourcePoint p, ResourcePoint.Tier tier) {
        if (!p.id().endsWith(OUTCROP)) {
            return;
        }
        ServerLevel level = ctx.level();
        BlockPos top = VillageContext.block(p.pos());
        BlockState stone = level.getBlockState(top.below());
        if (stone.isAir()) {
            return;
        }
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                BlockPos u = top.offset(ox, 0, oz);
                boolean corner = ox != 0 && oz != 0;
                boolean keepUpper = tier == ResourcePoint.Tier.FULL || (tier == ResourcePoint.Tier.HALF && !corner);
                if (!keepUpper && level.getBlockState(u).is(stone.getBlock())) {
                    level.setBlock(u, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                BlockPos d = u.below();
                if (tier == ResourcePoint.Tier.EMPTY && level.getBlockState(d).is(stone.getBlock())) {
                    level.setBlock(d, ((ox + oz) & 1) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
        }
    }

    /** 只供 GameTest（TC-T19-68）：按当前状态找一次天然资源点。 */
    public static boolean registerNaturalForTest(ServerLevel level, java.util.UUID village, CivVillageData.NaturalPoint np) {
        return VillageScheduler.contextForTest(level, village).map(ctx -> registerNatural(ctx, np)).orElse(false);
    }
}
