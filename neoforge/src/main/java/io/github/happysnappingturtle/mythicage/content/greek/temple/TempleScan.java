package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.core.greek.temple.ColumnCounter;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleGeometry;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.scan.Grid3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * 一座神坛的一轮扫描（T8a 设计文档 §3）：按 core 的格序（y → z → x）分批读取方块，把每格归类进几张位图，
 * 全部读完后交给 core 统计。只读已加载的区块；遇到未加载区块即中止，结果标记为「暂不可验证」。
 */
final class TempleScan {
    private final ServerLevel level;
    private final BlockPos altar;
    private final TempleRules rules;
    private final TempleGeometry geo;
    private final net.minecraft.core.Direction forward;
    private final net.minecraft.core.Direction right;
    private final Grid3 column;
    private final Grid3 cover;
    private final Grid3 floor;
    private final Grid3 full;
    private final Grid3 sturdyTop;
    private final Grid3 passable;
    /** 半格踏步：下半台阶、下半楼梯（修订 02）。 */
    private final Grid3 halfStep;
    private int litBraziers;
    private int statues;
    private int pediments;
    private final List<BlockPos> unlitBraziers = new ArrayList<>();
    private boolean unloaded;

    TempleScan(ServerLevel level, BlockPos altar, net.minecraft.core.Direction facing, TempleRules rules) {
        this.level = level;
        this.altar = altar;
        this.rules = rules;
        this.geo = new TempleGeometry(rules.scan());
        this.forward = facing;
        this.right = facing.getClockWise();
        this.column = geo.newGrid();
        this.cover = geo.newGrid();
        this.floor = geo.newGrid();
        this.full = geo.newGrid();
        this.sturdyTop = geo.newGrid();
        this.passable = geo.newGrid();
        this.halfStep = geo.newGrid();
    }

    int volume() {
        return rules.scan().volume();
    }

    /** 局部坐标（core 的格序：神坛在 (halfWidth, below, back)，+z 为神坛朝向）→ 世界坐标。 */
    BlockPos world(int lx, int ly, int lz) {
        TempleRules.Scan sc = rules.scan();
        return world(altar, forward, sc, lx, ly, lz);
    }

    static BlockPos world(BlockPos altar, net.minecraft.core.Direction forward, TempleRules.Scan sc, int lx, int ly, int lz) {
        net.minecraft.core.Direction right = forward.getClockWise();
        int dx = lx - sc.halfWidth();
        int dz = lz - sc.back();
        return altar.offset(right.getStepX() * dx + forward.getStepX() * dz, ly - sc.below(),
                right.getStepZ() * dx + forward.getStepZ() * dz);
    }

    /** 扫描体在世界中的包围盒（两个对角，包含）。 */
    static BlockPos[] bounds(BlockPos altar, net.minecraft.core.Direction forward, TempleRules.Scan sc) {
        BlockPos a = world(altar, forward, sc, 0, 0, 0);
        BlockPos b = world(altar, forward, sc, sc.sizeX() - 1, sc.sizeY() - 1, sc.sizeZ() - 1);
        return new BlockPos[]{new BlockPos(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()))};
    }

    /** 扫描体涉及的区块是否全部已加载（不主动加载区块）。 */
    boolean chunksLoaded() {
        BlockPos[] b = bounds(altar, forward, rules.scan());
        for (int cx = b[0].getX() >> 4; cx <= b[1].getX() >> 4; cx++) {
            for (int cz = b[0].getZ() >> 4; cz <= b[1].getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    void reset() {
        column.clear();
        cover.clear();
        floor.clear();
        full.clear();
        sturdyTop.clear();
        passable.clear();
        halfStep.clear();
        litBraziers = 0;
        statues = 0;
        pediments = 0;
        unlitBraziers.clear();
        unloaded = false;
    }

    void scan(int start, int count) {
        if (start == 0) {
            reset();
            unloaded = !chunksLoaded();
        }
        if (unloaded) {
            return;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = start; i < start + count; i++) {
            int[] c = column.coords(i);
            p.set(world(c[0], c[1], c[2]));
            if (!level.isLoaded(p)) {
                unloaded = true;
                return;
            }
            classify(i, p, level.getBlockState(p));
        }
    }

    private void classify(int i, BlockPos p, BlockState s) {
        if (s.isAir()) {
            passable.setIndex(i, true);
            return;
        }
        if (s.is(GreekTemple.GREEK_COLUMN)) {
            column.setIndex(i, true);
        }
        if (s.is(GreekTemple.TEMPLE_FLOOR)) {
            floor.setIndex(i, true);
        }
        boolean top = s.isFaceSturdy(level, p, Direction.UP);
        if ((top || s.isFaceSturdy(level, p, Direction.DOWN)) && !s.is(GreekTemple.ROOF_EXCLUDED)) {
            cover.setIndex(i, true);
        }
        sturdyTop.setIndex(i, top);
        full.setIndex(i, s.isCollisionShapeFullBlock(level, p));
        passable.setIndex(i, s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty());
        if (isHalfStep(s)) {
            halfStep.setIndex(i, true);
        }
        if (s.is(GreekTemple.BRAZIERS)) {
            boolean lit = !s.hasProperty(BlockStateProperties.LIT) || s.getValue(BlockStateProperties.LIT);
            if (lit) {
                litBraziers++;
            } else {
                unlitBraziers.add(p.immutable());
            }
        }
        if (s.is(GreekTemple.TEMPLE_STATUES) && (!s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)) {
            statues++;
        }
        if (s.is(GreekTemple.TEMPLE_PEDIMENTS)) {
            pediments++;
        }
    }

    private static boolean isHalfStep(BlockState s) {
        if (s.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return s.getValue(BlockStateProperties.SLAB_TYPE) == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM;
        }
        return s.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                && s.getValue(BlockStateProperties.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM;
    }

    boolean unloaded() {
        return unloaded;
    }

    /** 高亮用的坐标（世界坐标），每类最多 {@link #HIGHLIGHT_LIMIT} 个。 */
    /**
     * @param shortColumns 高度不足的柱，以及（修订 01）不在台基上的柱
     * @param altarCover   神坛头顶的遮挡（至多 1 个）
     */
    record Highlights(List<BlockPos> uncoveredRoof, List<BlockPos> countedColumns, List<BlockPos> shortColumns,
                      List<BlockPos> unlitBraziers, List<BlockPos> prayerSlots, List<BlockPos> altarCover) {
    }

    static final int HIGHLIGHT_LIMIT = 128;

    record Outcome(TempleSurvey survey, List<BlockPos> prayerSlotsAll, Highlights highlights) {
    }

    /** 全部读完后的统计；prayerSlotLimitMax 为各等级中最多的祈祷位数。 */
    Outcome finish(int prayerSlotLimitMax) {
        ColumnCounter.Result columns = ColumnCounter.count(column, rules.columnMinHeight());
        TempleGeometry.ColumnBox box = TempleGeometry.columnBox(columns);
        TempleGeometry.Roof roof = geo.roof(cover, column, box, rules.roof());
        Grid3 reachable = geo.reachable(geo.standable(sturdyTop, passable, halfStep), halfStep);
        int marble = geo.reachableFloor(floor, reachable, halfStep);
        TempleSurvey survey = new TempleSurvey(columns.count(), geo.raisedColumns(columns), roof.covered(), roof.area(),
                geo.openAltar(cover), geo.platformReachable(reachable, box), litBraziers, statues, pediments, marble);
        List<int[]> slots = geo.prayerSlots(reachable, prayerSlotLimitMax);

        List<BlockPos> uncovered = new ArrayList<>();
        for (int[] c : roof.uncovered()) {
            add(uncovered, world(c[0], c[1], c[2]));
        }
        int groundY = rules.scan().below();
        List<BlockPos> counted = new ArrayList<>();
        List<BlockPos> weak = new ArrayList<>();
        for (ColumnCounter.Group g : columns.groups()) {
            int[] b = g.base();
            // 绿：站在台基上的合格柱；黄：高度不足或贴地的柱（中神庙起需要站上台基）
            add(g.counted() && b[1] >= groundY + 1 ? counted : weak, world(b[0], b[1], b[2]));
        }
        List<BlockPos> slotPos = new ArrayList<>();
        for (int[] s : slots) {
            slotPos.add(world(s[0], s[1], s[2]));
        }
        int[] ac = geo.altarCover(cover);
        List<BlockPos> altarCover = ac == null ? List.of() : List.of(world(ac[0], ac[1], ac[2]));
        List<BlockPos> unlit = unlitBraziers.size() > HIGHLIGHT_LIMIT
                ? unlitBraziers.subList(0, HIGHLIGHT_LIMIT) : unlitBraziers;
        return new Outcome(survey, slotPos,
                new Highlights(uncovered, counted, weak, List.copyOf(unlit), slotPos, altarCover));
    }

    private static void add(List<BlockPos> list, BlockPos p) {
        if (list.size() < HIGHLIGHT_LIMIT) {
            list.add(p);
        }
    }
}
