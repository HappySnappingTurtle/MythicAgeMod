package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.found.SiteFinder;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 临时找地（T19 设计文档 §1.13 K7，V0 §5.4，D6）：没有预留地块的村庄（奠基、立原版村庄），或生成村庄里没有合适地块的建筑，
 * 在已建范围外 8–32 格找一块地（{@link SiteFinder}），登记成临时地块追加进规划数据，整平后走原来的施工流程。
 * 整平只动自然方块：低处用泥土垫到整平高度，高出的自然方块削掉，每列最多 {@link #PAD} 格。
 */
public final class AdHocSites {
    public static final String PREFIX = "adhoc_";
    public static final int PAD = 2;

    private AdHocSites() {
    }

    /** 自然地面（能当地基、能削）：泥土类、石头、沙、砂砾、雪、黏土、陶瓦岩。 */
    static boolean naturalGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)
                || s.is(BlockTags.SNOW) || s.is(Blocks.CLAY) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.MUD);
    }

    /** 地面上可以直接清掉的自然东西：空气、草、花、雪层、树叶。 */
    static boolean clearable(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(Blocks.SNOW);
    }

    /** 一列地形：地面高度（最高的非树叶实体方块）、是不是水、是不是玩家方块（或别的村庄的已建范围、树干）。 */
    static SiteFinder.Column column(ServerLevel level, int x, int z, List<Box> others) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos p = new BlockPos(x, top, z);
        BlockState g = level.getBlockState(p);
        boolean water = !g.getFluidState().isEmpty();
        boolean other = others.stream().anyMatch(b -> b.containsXZ(x, z));
        boolean player = !water && (other || !naturalGround(g));
        return new SiteFinder.Column(top, water, player);
    }

    /**
     * 找地并开项目。找不到地就记下「需要空地」（村务界面显示），返回 empty。
     *
     * @param want 建筑（及次神祠的神）；地块 id 由这里分配
     */
    public static Optional<BuildProject> start(VillageContext ctx, ProjectPicker.Pick want) {
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        BuildingSpec base = specs.get(want.building());
        if (base == null) {
            return Optional.empty();
        }
        ServerLevel level = ctx.level();
        VillagePlans plans = VillagePlans.get(level.getServer());
        List<Box> own = ctx.plan().info().built();
        List<Box> others = new ArrayList<>();
        for (VillagePlans.Plan p : plans.all()) {
            if (!p.village().equals(ctx.village().id()) && p.dim().equals(level.dimension())) {
                others.addAll(p.info().built());
            }
        }
        int size = Math.max(base.sizeX(), base.sizeZ());
        Map<Long, SiteFinder.Column> cache = new HashMap<>();
        Optional<SiteFinder.Site> site = SiteFinder.find(own, size, size,
                (x, z) -> cache.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> column(level, x, z, others)));
        if (site.isEmpty()) {
            VillageScheduler.runtime(ctx.village().id()).setNeedsSpace(base.id());
            return Optional.empty();
        }
        VillageScheduler.runtime(ctx.village().id()).setNeedsSpace("");
        Box b = site.get().box();
        int ground = site.get().ground();
        BlockPos c = ctx.center();
        double dx = c.getX() - (b.minX() + b.maxX()) / 2.0;
        double dz = c.getZ() - (b.minZ() + b.maxZ()) / 2.0;
        String front = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
        VillageInfoPiece.Info info = ctx.plan().info();
        int n = (int) info.plots().stream().filter(p -> p.id().startsWith(PREFIX)).count() + 1;
        String id = PREFIX + n;
        String kind = base.plotKinds().isEmpty() ? "adhoc" : base.plotKinds().iterator().next();
        Box plotBox = new Box(b.minX(), ground + 1, b.minZ(), b.maxX(), ground + 1, b.maxZ());
        List<VillagePlan.Plot> plots = new ArrayList<>(info.plots());
        plots.add(new VillagePlan.Plot(id, kind, base.stage(), plotBox, front));
        plans.updateInfo(ctx.village().id(), new VillageInfoPiece.Info(info.civ(), info.patron(), info.layout(), info.layoutVersion(),
                info.templateSet(), info.center(), info.anchors(), info.built(), plots));
        pad(level, plotBox.inflateXZ(1), ground);
        Optional<VillageContext> fresh = VillageScheduler.contextOf(level, ctx.village().id());
        return fresh.flatMap(f -> Construction.start(f, new ProjectPicker.Pick(base.id(), id, want.god(), "")));
    }

    /** 整平：每列在整平高度以下垫土（最多 {@link #PAD} 格），以上削掉自然方块（最多 {@link #PAD} + 1 格）。 */
    static void pad(ServerLevel level, Box b, int ground) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                for (int y = ground - PAD; y <= ground; y++) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (clearable(s) && s.getFluidState().isEmpty()) {
                        level.setBlock(m, (y == ground ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                for (int y = ground + 1; y <= ground + PAD + 1; y++) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (!s.isAir() && (naturalGround(s) || clearable(s)) && s.getFluidState().isEmpty()) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
