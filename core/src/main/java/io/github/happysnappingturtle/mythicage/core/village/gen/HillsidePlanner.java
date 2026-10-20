package io.github.happysnappingturtle.mythicage.core.village.gen;

import io.github.happysnappingturtle.mythicage.core.village.VillageRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * 希腊村庄规划器（T13b §4）：卫城在局部高点、门朝东；广场在下坡；坡地小屋围着广场、门朝广场；
 * 谷仓、陶坊、农园在村边；主神件；村口与道路（必要时架桥）；各阶段的预留地块。
 * 全部基于一次采样的高度网格（{@link HeightGrid}），同一种子同一位置结果相同。
 */
public final class HillsidePlanner {
    public static final String ID = "hillside";
    public static final int VERSION = 1;
    public static final int RADIUS = 72;
    public static final int STEP = 8;
    /** 建筑之间至少留的空隙。 */
    public static final int GAP = 2;
    public static final int MIN_HOUSES = 3;
    /** 村口主路从广场边起至少这么长（越过民居环）。 */
    public static final int MAX_MAIN_ROAD = 48;
    public static final int MAIN_ROAD = 34;
    public static final List<String> REQUIRED = List.of("center", "guardian", "home", "entrance");

    /**
     * 规划篇 §3.1 的预留地块。每阶段第一块次神祠是必需的（仪式要立祠）；其余放不下时不作废整座村庄，
     * 记在 {@link VillagePlan} 之外，由 T19 按 V0 §5.4 在已建范围外临时找地。
     */
    public record PlotSpec(String kind, String stage, int w, int d, int count, boolean optional) {
    }

    public static final List<PlotSpec> PLOTS = List.of(
            new PlotSpec("shrine", "classical", 11, 11, 1, false),
            new PlotSpec("shrine", "heroic", 11, 11, 1, false),
            new PlotSpec("shrine", "mythic", 11, 11, 1, false),
            new PlotSpec("market", "classical", 21, 7, 1, true),
            new PlotSpec("shrine", "classical", 11, 11, 1, true),
            new PlotSpec("workshop", "classical", 9, 9, 1, true),
            new PlotSpec("storage", "classical", 11, 9, 1, true),
            new PlotSpec("house", "classical", 11, 11, 3, true),
            new PlotSpec("shrine", "heroic", 11, 11, 1, true),
            new PlotSpec("training", "heroic", 15, 15, 1, true),
            new PlotSpec("workshop", "heroic", 9, 11, 1, true),
            new PlotSpec("house", "heroic", 11, 11, 3, true),
            new PlotSpec("theater", "heroic", 21, 17, 1, true),
            new PlotSpec("shrine", "mythic", 11, 11, 1, true),
            new PlotSpec("workshop", "mythic", 9, 9, 1, true),
            new PlotSpec("feature", "mythic", 11, 11, 1, true));

    private final HeightGrid grid;
    private final TerrainSampler terrain;
    private final Map<String, BuildingSpec> specs;
    private final String civ;
    private final String patron;
    private final SplittableRandom rnd;
    private final List<String> problems;

    private final List<Box> occupied = new ArrayList<>();
    private final List<VillagePlan.Placement> placed = new ArrayList<>();
    private final Map<String, List<Pos>> anchors = new LinkedHashMap<>();
    private final List<VillagePlan.Plot> plots = new ArrayList<>();
    private final List<VillagePlan.Path> paths = new ArrayList<>();
    /** 每座建筑门外一格（道路终点）与路宽。 */
    private record Door(Pos pos, int width) {
    }

    private final List<Door> doors = new ArrayList<>();
    private Box agora;
    private Box acropolis;
    private int acroGround;

    private HillsidePlanner(HeightGrid grid, TerrainSampler terrain, Map<String, BuildingSpec> specs, String civ, String patron,
                            long seed, List<String> problems) {
        this.grid = grid;
        this.terrain = terrain;
        this.specs = specs;
        this.civ = civ;
        this.patron = patron;
        this.rnd = new SplittableRandom(seed);
        this.problems = problems;
    }

    public static Optional<VillagePlan> plan(int sx, int sz, TerrainSampler t, Map<String, BuildingSpec> specs, String civ,
                                             String patron, List<String> problems) {
        HeightGrid g = new HeightGrid(t, sx, sz, RADIUS, STEP);
        long seed = Double.doubleToLongBits(VillageRules.unit(sx, 0, sz, 0x48_49_4C_4CL));
        return new HillsidePlanner(g, t, specs, civ, patron, seed, problems).run();
    }

    private BuildingSpec spec(String shortId) {
        BuildingSpec s = specs.get(civ + ":" + shortId);
        if (s == null) {
            throw new IllegalStateException("缺少建筑数据：" + civ + ":" + shortId);
        }
        return s;
    }

    /** 规划范围里几乎全是水就不建村（能否放下其余交给逐座判断）。 */
    public static final double MAX_WATER = 0.85;

    private Optional<VillagePlan> run() {
        if (grid.waterFraction() > MAX_WATER) {
            problems.add("水面太多");
            return Optional.empty();
        }
        if (!acropolis() || !agora()) {
            return Optional.empty();
        }
        if (!entrance()) {
            problems.add("村口：四个方向都走不通");
            return Optional.empty();
        }
        sideOf(agora, spec("krene"), 3);
        if (patron.equals("zeus")) {
            around(spec("zeus_oak_archaic"), center(acropolis), 16, 40, 6, 1, 0);
        } else if (patron.equals("poseidon") && !harbor() && !sideOf(agora, spec("poseidon_spring"), 3)) {
            around(spec("poseidon_spring"), center(agora), 14, 34, 3, 1, 0);
        } else if (patron.equals("hades")) {
            cemetery();
        }
        int houses = 4 + rnd.nextInt(3);
        int n = around(spec("house_small"), center(agora), 15, 46, 5, houses, 0);
        if (n < MIN_HOUSES) {
            problems.add("住居不足：" + n);
            return Optional.empty();
        }
        if (around(spec("granary"), center(agora), 16, 60, 5, 1, 0) < 1
                || around(spec("pottery"), center(agora), 20, 66, 6, 1, 2) < 1) {
            problems.add("缺件：谷仓或陶坊");
            return Optional.empty();
        }
        around(spec("olive_grove"), center(agora), 26, 66, 6, 1, 0);
        around(spec("wheat_field"), center(agora), 26, 66, 3, 1, 0);
        if (rnd.nextDouble() < 0.6) {
            around(spec("vineyard"), center(agora), 28, 66, 5, 1, 0);
        }
        if (rnd.nextDouble() < 0.6) {
            around(spec("pen"), center(agora), 22, 60, 4, 1, 0);
        }
        if (!reservePlots()) {
            return Optional.empty();
        }
        roads();
        List<Pos> center = anchors.getOrDefault("center", List.of());
        if (center.isEmpty()) {
            problems.add("缺件：锚点 center");
            return Optional.empty();
        }
        VillagePlan plan = new VillagePlan(civ, patron, ID, VERSION, center.get(0), placed, anchors, plots, paths);
        List<String> bad = PlanValidator.validate(plan, REQUIRED);
        for (VillagePlan.Plot p : plots) {
            if (p.box().farthestDistSq(plan.center().x(), plan.center().z()) > (long) PlanValidator.MAX_RANGE * PlanValidator.MAX_RANGE) {
                bad.add("出界：地块 " + p.id());
            }
        }
        if (!bad.isEmpty()) {
            problems.addAll(bad);
            return Optional.empty();
        }
        return Optional.of(plan);
    }

    // ───────────── 卫城与广场 ─────────────

    private boolean acropolis() {
        beginStep();
        BuildingSpec a = spec("acropolis_archaic");
        record Cand(int x, int z, double score) {
        }
        List<Cand> cands = new ArrayList<>();
        int sx = grid.centerX();
        int sz = grid.centerZ();
        for (int dx = -32; dx <= 32; dx += STEP) {
            for (int dz = -32; dz <= 32; dz += STEP) {
                int x = sx + dx;
                int z = sz + dz;
                Box foot = footAt(a, x, z, Turn.R0);
                HeightGrid.Footprint f = grid.footprint(foot);
                cands.add(new Cand(x, z, f.ground() - 1.5 * f.relief() - Math.hypot(dx, dz) / 10.0));
            }
        }
        cands.sort(Comparator.comparingDouble(Cand::score).reversed());
        for (Cand c : cands) {
            Box foot = footAt(a, c.x(), c.z(), Turn.R0);
            if (!fits(foot, 8) || !confirm(foot, 8)) {
                continue;
            }
            Box ext = new Box(foot.minX() - 9, 0, foot.minZ() + 11, foot.minX() - 1, 0, foot.maxZ());
            int tz = foot.minZ() + 18;
            Box altar = new Box(foot.maxX() + 2, 0, tz - 6, foot.maxX() + 14, 0, tz + 6);
            if (!fits(ext, 8) || !fits(altar, 8)) {
                continue;
            }
            VillagePlan.Placement p = commit(a, foot, Turn.R0);
            acropolis = p.box();
            acroGround = groundAt(foot);
            plot("classical_temple_ext", "temple_ext", "classical", ext, "east");
            plot("mythic_altar", "altar", "mythic", altar, "east");
            return true;
        }
        problems.add("卫城：没有合格的高点");
        return false;
    }

    private boolean agora() {
        beginStep();
        BuildingSpec a = spec("agora");
        Pos ac = center(acropolis);
        record Cand(int x, int z, double score) {
        }
        List<Cand> cands = new ArrayList<>();
        for (int r = 24; r <= 36; r += 3) {
            for (int deg = 0; deg < 360; deg += 15) {
                int x = ac.x() + (int) Math.round(r * Math.cos(Math.toRadians(deg)));
                int z = ac.z() + (int) Math.round(r * Math.sin(Math.toRadians(deg)));
                HeightGrid.Footprint f = grid.footprint(footAt(a, x, z, Turn.R0));
                double lower = Math.clamp(acroGround - f.ground(), 0, 10);
                cands.add(new Cand(x, z, lower - 2.0 * f.relief() - Math.abs(r - 28) / 6.0));
            }
        }
        cands.sort(Comparator.comparingDouble(Cand::score).reversed());
        for (Cand c : cands) {
            Box foot = footAt(a, c.x(), c.z(), Turn.R0);
            if (fits(foot, 4) && confirm(foot, 4)) {
                agora = commit(a, foot, Turn.R0).box();
                return true;
            }
        }
        problems.add("广场：没有合格的平地");
        return false;
    }

    // ───────────── 通用放置 ─────────────

    private static Pos center(Box b) {
        return new Pos(b.centerX(), b.minY(), b.centerZ());
    }

    private Box footAt(BuildingSpec s, int cx, int cz, Turn t) {
        int[] size = TemplateTransform.rotatedSize(s.sizeX(), s.sizeZ(), t);
        int minX = cx - size[0] / 2;
        int minZ = cz - size[1] / 2;
        return new Box(minX, 0, minZ, minX + size[0] - 1, 0, minZ + size[1] - 1);
    }

    /** 能放：在网格范围内、不压别的建筑（含空隙）、不泡水、高差不超过 maxRelief。 */
    private boolean fits(Box foot, int maxRelief) {
        if (!grid.inside(foot.minX(), foot.minZ()) || !grid.inside(foot.maxX(), foot.maxZ())) {
            return false;
        }
        for (Box o : occupied) {
            if (o.inflateXZ(GAP).intersectsXZ(foot)) {
                return false;
            }
        }
        HeightGrid.Footprint f = grid.footprint(foot);
        // 判水时外扩半个网格：网格间距 8，窄河可能正好落在两个采样点之间
        if (f.wet() || grid.footprint(foot.inflateXZ(STEP / 2)).wet() || f.relief() > maxRelief) {
            return false;
        }
        // 网格最近点会低估陡坡：再用地形采样直接量一次四角与中心（估算，便宜）
        int[] hs = corners(foot);
        return hs[4] - hs[0] <= maxRelief + 1;
    }

    private final Map<Box, int[]> cornerCache = new java.util.HashMap<>();

    /** 占地中心与四角的地面（估算，升序），同一块地只量一次；下标 5 是中心。 */
    private int[] corners(Box foot) {
        return cornerCache.computeIfAbsent(new Box(foot.minX(), 0, foot.minZ(), foot.maxX(), 0, foot.maxZ()), k -> {
            int c = terrain.floor(foot.centerX(), foot.centerZ());
            int[] hs = {c, terrain.floor(foot.minX(), foot.minZ()), terrain.floor(foot.maxX(), foot.minZ()),
                    terrain.floor(foot.minX(), foot.maxZ()), terrain.floor(foot.maxX(), foot.maxZ())};
            java.util.Arrays.sort(hs);
            return new int[]{hs[0], hs[1], hs[2], hs[3], hs[4], c};
        });
    }

    private final Map<Box, int[]> preciseCache = new java.util.HashMap<>();

    /** 占地中心与四角的较准地面（升序），同一块地只量一次。 */
    private int[] precise(Box foot) {
        return preciseCache.computeIfAbsent(new Box(foot.minX(), 0, foot.minZ(), foot.maxX(), 0, foot.maxZ()), k -> {
            int[] hs = {terrain.ground(foot.centerX(), foot.centerZ()), terrain.ground(foot.minX(), foot.minZ()),
                    terrain.ground(foot.maxX(), foot.minZ()), terrain.ground(foot.minX(), foot.maxZ()),
                    terrain.ground(foot.maxX(), foot.maxZ())};
            java.util.Arrays.sort(hs);
            return hs;
        });
    }

    /** 每个放置步骤里因较准复核换位置的次数上限；超过后不再复核（山地上避免反复扫描）。 */
    public static final int MAX_CONFIRM_REJECTS = 3;
    private int confirmRejects;

    /** 新的放置步骤开始：复核计数清零。 */
    private void beginStep() {
        confirmRejects = 0;
    }

    /** 选中的位置用较准地面复核坡度（估算会在几十格内起伏，坡度可能被低估）。 */
    private boolean confirm(Box foot, int maxRelief) {
        if (confirmRejects >= MAX_CONFIRM_REJECTS) {
            return true;
        }
        int[] hs = precise(foot);
        if (hs[4] - hs[0] <= maxRelief + 2) {
            return true;
        }
        confirmRejects++;
        return false;
    }

    /** 落位地面：占地中心与四角较准地面的中位数（较准扫描误差 p90 2 格；估算只用来挑地方）。 */
    private int groundAt(Box foot) {
        return precise(foot)[2];
    }

    private VillagePlan.Placement commit(BuildingSpec s, Box foot, Turn t) {
        int ground = groundAt(foot);
        Pos origin = TemplateTransform.originFor(new Pos(foot.minX(), ground - s.floorY(), foot.minZ()), s.sizeX(), s.sizeZ(), t);
        Box box = TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t);
        VillagePlan.Placement p = new VillagePlan.Placement(s.id(), s.template(), origin, t, box);
        placed.add(p);
        occupied.add(box);
        s.anchors().forEach((name, pts) -> pts.forEach(rel -> anchor(name, TemplateTransform.toWorld(origin, rel, t))));
        String facing = t.rotateFacing(s.doorFacing());
        Pos door = TemplateTransform.toWorld(origin, s.door(), t);
        int[] d = dir(facing);
        doors.add(new Door(new Pos(door.x() + d[0], ground, door.z() + d[1]), s.id().endsWith(":acropolis_archaic") ? 3 : 2));
        return p;
    }

    private void anchor(String name, Pos p) {
        anchors.computeIfAbsent(name, k -> new ArrayList<>()).add(p);
    }

    private void plot(String id, String kind, String stage, Box foot, String front) {
        int ground = groundAt(foot);
        Box box = new Box(foot.minX(), ground, foot.minZ(), foot.maxX(), ground, foot.maxZ());
        plots.add(new VillagePlan.Plot(id, kind, stage, box, front));
        occupied.add(box);
    }

    static int[] dir(String facing) {
        return switch (facing) {
            case "north" -> new int[]{0, -1};
            case "south" -> new int[]{0, 1};
            case "east" -> new int[]{1, 0};
            default -> new int[]{-1, 0};
        };
    }

    /** 从 (px, pz) 看向 (tx, tz) 的大致朝向（取水平分量较大的轴）。 */
    static String toward(int px, int pz, int tx, int tz) {
        int dx = tx - px;
        int dz = tz - pz;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? "east" : "west";
        }
        return dz >= 0 ? "south" : "north";
    }

    /**
     * 在 (cx, cz) 周围半径 rMin–rMax 的环上找地方放 count 座，门朝中心；waterBonus > 0 时附近有水的位置加分（陶坊靠近黏土）。
     * 返回放下的数量。
     */
    private int around(BuildingSpec s, Pos c, int rMin, int rMax, int maxRelief, int count, double waterBonus) {
        beginStep();
        record Cand(int x, int z, String facing, double score) {
        }
        List<Cand> cands = new ArrayList<>();
        double phase = rnd.nextDouble() * 360;
        for (int r = rMin; r <= rMax; r += 3) {
            for (double deg = phase; deg < phase + 360; deg += 18) {
                int x = c.x() + (int) Math.round(r * Math.cos(Math.toRadians(deg)));
                int z = c.z() + (int) Math.round(r * Math.sin(Math.toRadians(deg)));
                String facing = toward(x, z, c.x(), c.z());
                HeightGrid.Footprint f = grid.footprint(footAt(s, x, z, Turn.between(s.doorFacing(), facing)));
                double score = -2.0 * f.relief() - r / 5.0 + rnd.nextDouble() * 0.5
                        + (waterBonus > 0 && grid.waterNear(x, z, 16) > 0 ? waterBonus : 0);
                cands.add(new Cand(x, z, facing, score));
            }
        }
        cands.sort(Comparator.comparingDouble(Cand::score).reversed());
        int n = 0;
        for (Cand k : cands) {
            if (n >= count) {
                break;
            }
            Turn t = Turn.between(s.doorFacing(), k.facing());
            Box foot = footAt(s, k.x(), k.z(), t);
            if (fits(foot, maxRelief) && confirm(foot, maxRelief)) {
                commit(s, foot, t);
                n++;
            }
        }
        return n;
    }

    /** 贴着某座建筑的一边放（泉屋、海神泉、集市地块），门朝它。 */
    private boolean sideOf(Box target, BuildingSpec s, int maxRelief) {
        beginStep();
        Pos c = center(target);
        List<String> sides = new ArrayList<>(List.of("west", "east", "north", "south"));
        java.util.Collections.shuffle(sides, new java.util.Random(rnd.nextLong()));
        for (String side : sides) {
            int[] d = dir(side);
            String facing = toward(c.x() + d[0], c.z() + d[1], c.x(), c.z()).equals(side) ? side : opposite(side);
            Turn t = Turn.between(s.doorFacing(), facing);
            int[] size = TemplateTransform.rotatedSize(s.sizeX(), s.sizeZ(), t);
            int x = d[0] < 0 ? target.minX() - GAP - 1 - size[0] / 2 : d[0] > 0 ? target.maxX() + GAP + 1 + size[0] / 2 : c.x();
            int z = d[1] < 0 ? target.minZ() - GAP - 1 - size[1] / 2 : d[1] > 0 ? target.maxZ() + GAP + 1 + size[1] / 2 : c.z();
            Box foot = footAt(s, x, z, t);
            if (fits(foot, maxRelief) && confirm(foot, maxRelief)) {
                commit(s, foot, t);
                return true;
            }
        }
        return false;
    }

    static String opposite(String f) {
        return switch (f) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    // ───────────── 主神件 ─────────────

    /** 波塞冬码头：陆地部分（模板 z 0–7）干燥平整、海的部分（z 8–20）全是水，且陆地比海面高 0–2 格。 */
    private boolean harbor() {
        if (grid.waterFraction() < 0.08) {
            return false;
        }
        BuildingSpec s = spec("poseidon_harbor");
        Pos ac = center(agora);
        for (int r = 18; r <= 64; r += 4) {
            for (int deg = 0; deg < 360; deg += 20) {
                int x = ac.x() + (int) Math.round(r * Math.cos(Math.toRadians(deg)));
                int z = ac.z() + (int) Math.round(r * Math.sin(Math.toRadians(deg)));
                for (String facing : List.of("north", "south", "east", "west")) {
                    Turn t = Turn.between(s.doorFacing(), facing);
                    Box foot = footAt(s, x, z, t);
                    Pos origin = TemplateTransform.originFor(new Pos(foot.minX(), 0, foot.minZ()), s.sizeX(), s.sizeZ(), t);
                    Box land = Box.of(TemplateTransform.toWorld(origin, new Pos(0, 0, 0), t),
                            TemplateTransform.toWorld(origin, new Pos(s.sizeX() - 1, 0, 6), t));
                    Box sea = Box.of(TemplateTransform.toWorld(origin, new Pos(0, 0, 10), t),
                            TemplateTransform.toWorld(origin, new Pos(s.sizeX() - 1, 0, s.sizeZ() - 1), t));
                    if (!grid.inside(sea.minX(), sea.minZ()) || !grid.inside(sea.maxX(), sea.maxZ())
                            || !fitsLand(land) || !grid.allWater(sea) || overlaps(foot)) {
                        continue;
                    }
                    int ground = groundAt(land);
                    int seaSurface = grid.surface(sea.centerX(), sea.centerZ());
                    if (ground - seaSurface < 0 || ground - seaSurface > 2) {
                        continue;
                    }
                    Pos o = TemplateTransform.originFor(new Pos(foot.minX(), ground - s.floorY(), foot.minZ()), s.sizeX(), s.sizeZ(), t);
                    Box box = TemplateTransform.worldBox(o, s.sizeX(), s.sizeY(), s.sizeZ(), t);
                    placed.add(new VillagePlan.Placement(s.id(), s.template(), o, t, box));
                    occupied.add(box);
                    s.anchors().forEach((name, pts) -> pts.forEach(rel -> anchor(name, TemplateTransform.toWorld(o, rel, t))));
                    Pos door = TemplateTransform.toWorld(o, s.door(), t);
                    int[] d = dir(t.rotateFacing(s.doorFacing()));
                    doors.add(new Door(new Pos(door.x() + d[0], ground, door.z() + d[1]), 2));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fitsLand(Box land) {
        HeightGrid.Footprint f = grid.footprint(land);
        if (!grid.inside(land.minX(), land.minZ()) || !grid.inside(land.maxX(), land.maxZ()) || f.wet() || f.relief() > 3) {
            return false;
        }
        int[] hs = corners(land);
        return hs[4] - hs[0] <= 4 && confirm(land, 3);
    }

    private boolean overlaps(Box foot) {
        for (Box o : occupied) {
            if (o.inflateXZ(GAP).intersectsXZ(foot)) {
                return true;
            }
        }
        return false;
    }

    /** 哈迪斯墓园：村口路边（史实：墓地在城外沿路）。 */
    private void cemetery() {
        beginStep();
        BuildingSpec s = spec("hades_cemetery");
        Pos e = anchors.get("entrance").get(0);
        Pos ac = center(agora);
        boolean alongX = Math.abs(e.x() - ac.x()) >= Math.abs(e.z() - ac.z());
        for (int side : new int[]{1, -1}) {
            for (int back = 6; back <= 24; back += 6) {
                int mx = alongX ? e.x() - Integer.signum(e.x() - ac.x()) * back : e.x() + side * 15;
                int mz = alongX ? e.z() + side * 15 : e.z() - Integer.signum(e.z() - ac.z()) * back;
                String facing = alongX ? (side > 0 ? "north" : "south") : (side > 0 ? "west" : "east");
                Turn t = Turn.between(s.doorFacing(), facing);
                Box foot = footAt(s, mx, mz, t);
                if (fits(foot, 5) && confirm(foot, 5)) {
                    commit(s, foot, t);
                    return;
                }
            }
        }
    }

    // ───────────── 村口、道路、桥 ─────────────

    /** 广场某一边门口外一格的点（道路起点）。 */
    private Pos opening(String side) {
        Pos c = center(agora);
        int[] d = dir(side);
        int x = d[0] < 0 ? agora.minX() - 1 : d[0] > 0 ? agora.maxX() + 1 : c.x();
        int z = d[1] < 0 ? agora.minZ() - 1 : d[1] > 0 ? agora.maxZ() + 1 : c.z();
        return new Pos(x, grid.surface(x, z), z);
    }

    /** 村口：沿离开卫城的轴向修主路；路上不能压建筑，最多跨一段 ≤ 11 格的水（架桥）。 */
    private boolean entrance() {
        Pos ac = center(acropolis);
        Pos gc = center(agora);
        List<String> sides = new ArrayList<>(List.of("north", "south", "east", "west"));
        sides.sort(Comparator.comparingDouble(f -> {
            int[] d = dir(f);
            return -(d[0] * (gc.x() - ac.x()) + d[1] * (gc.z() - ac.z()));
        }));
        for (String side : sides) {
            int[] d = dir(side);
            Pos start = opening(side);
            int len = 0;
            int waterStart = -1;
            int waterEnd = -1;
            boolean ok = true;
            int maxLen = Math.min(MAX_MAIN_ROAD, RADIUS - 2 - Math.max(Math.abs(start.x() - grid.centerX()),
                    Math.abs(start.z() - grid.centerZ())));
            int clear = 0;
            for (int i = 1; i <= maxLen; i++) {
                int x = start.x() + d[0] * i;
                int z = start.z() + d[1] * i;
                Box cell = new Box(x - Math.abs(d[1]), 0, z - Math.abs(d[0]), x + Math.abs(d[1]), 0, z + Math.abs(d[0]));
                boolean blocked = false;
                for (Box o : occupied) {
                    if (!o.equals(agora) && o.intersectsXZ(cell)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) {
                    ok = false;
                    break;
                }
                if (grid.water(x, z)) {
                    if (waterStart < 0) {
                        waterStart = i;
                    } else if (waterEnd >= 0) {
                        ok = false; // 第二段水
                        break;
                    }
                    clear = 0;
                } else {
                    if (waterStart >= 0 && waterEnd < 0) {
                        waterEnd = i - 1;
                    }
                    clear++;
                }
                len = i;
                if (i >= MAIN_ROAD && clear >= 4) {
                    break;
                }
            }
            if (!ok || len < MAIN_ROAD / 2 || (waterStart >= 0 && (waterEnd < 0 || waterEnd - waterStart + 1 > 11))) {
                continue;
            }
            Pos end = new Pos(start.x() + d[0] * len, grid.surface(start.x() + d[0] * len, start.z() + d[1] * len),
                    start.z() + d[1] * len);
            if (waterStart >= 0 && !bridge(start, d, waterStart, waterEnd)) {
                continue;
            }
            anchor("entrance", end);
            paths.add(new VillagePlan.Path(3, List.of(start, end)));
            occupied.add(Box.of(new Pos(start.x() - 2 * Math.abs(d[1]), 0, start.z() - 2 * Math.abs(d[0])),
                    new Pos(end.x() + 2 * Math.abs(d[1]), 0, end.z() + 2 * Math.abs(d[0]))));
            return true;
        }
        return false;
    }

    private boolean bridge(Pos start, int[] d, int waterStart, int waterEnd) {
        int run = waterEnd - waterStart + 1;
        BuildingSpec s = spec(run <= 5 ? "bridge_narrow" : "bridge_wide");
        List<Pos> cross = s.anchors("crossing");
        if (cross.size() < 2) {
            return false;
        }
        String facing = d[0] > 0 ? "west" : d[0] < 0 ? "east" : d[1] > 0 ? "north" : "south";
        Turn t = Turn.between(s.doorFacing(), facing);
        Pos mid = new Pos((cross.get(0).x() + cross.get(1).x()) / 2, 0, cross.get(0).z());
        int mi = (waterStart + waterEnd) / 2;
        int wx = start.x() + d[0] * mi;
        int wz = start.z() + d[1] * mi;
        int bank = grid.surface(start.x() + d[0] * (waterStart - 1), start.z() + d[1] * (waterStart - 1));
        Pos rel = t.apply(mid);
        Pos origin = new Pos(wx - rel.x(), bank - s.floorY(), wz - rel.z());
        Box box = TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t);
        for (Box o : occupied) {
            if (!o.equals(agora) && o.intersectsXZ(box)) {
                return false;
            }
        }
        placed.add(new VillagePlan.Placement(s.id(), s.template(), origin, t, box));
        occupied.add(box);
        return true;
    }

    /** 广场到各建筑门口的支路（L 形，挑压到别的建筑少的走法）；卫城与村口的路宽 3，其余 2。 */
    private void roads() {
        Pos gc = center(agora);
        for (Door dr : doors) {
            Pos door = dr.pos();
            if (agora.containsXZ(door.x(), door.z())) {
                continue;
            }
            String side = toward(gc.x(), gc.z(), door.x(), door.z());
            Pos start = opening(side);
            if (Math.abs(start.x() - door.x()) + Math.abs(start.z() - door.z()) < 2) {
                continue;
            }
            Pos cornerA = new Pos(door.x(), grid.surface(door.x(), start.z()), start.z());
            Pos cornerB = new Pos(start.x(), grid.surface(start.x(), door.z()), door.z());
            Pos corner = crossings(start, cornerA, door) <= crossings(start, cornerB, door) ? cornerA : cornerB;
            paths.add(new VillagePlan.Path(dr.width(), List.of(start, corner, door)));
        }
    }

    private int crossings(Pos a, Pos b, Pos c) {
        int n = 0;
        for (Box o : occupied) {
            if (o.equals(agora)) {
                continue;
            }
            if (segmentHits(a, b, o) || segmentHits(b, c, o)) {
                n++;
            }
        }
        return n;
    }

    private static boolean segmentHits(Pos a, Pos b, Box o) {
        Box seg = new Box(Math.min(a.x(), b.x()), 0, Math.min(a.z(), b.z()), Math.max(a.x(), b.x()), 0, Math.max(a.z(), b.z()));
        return seg.intersectsXZ(o.inflateXZ(-1));
    }

    // ───────────── 预留地块 ─────────────

    private boolean reservePlots() {
        Pos gc = center(agora);
        Pos ac = center(acropolis);
        Map<String, Integer> counters = new LinkedHashMap<>();
        for (PlotSpec ps : PLOTS) {
            for (int i = 0; i < ps.count(); i++) {
                String id = ps.stage() + "_" + ps.kind() + "_" + counters.merge(ps.stage() + ps.kind(), 1, Integer::sum);
                boolean ok;
                if (ps.kind().equals("market")) {
                    ok = plotBeside(id, ps);
                } else if (ps.kind().equals("shrine")) {
                    int relief = ps.optional() ? 3 : 6;
                    ok = plotAround(id, ps, ac, 14, 44, 3, false) || plotAround(id, ps, gc, 16, 74, relief, false);
                } else {
                    ok = plotAround(id, ps, gc, 16, 74, 3, ps.kind().equals("theater"));
                }
                if (!ok && !ps.optional()) {
                    problems.add("缺地块：" + id);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean plotBeside(String id, PlotSpec ps) {
        beginStep();
        Pos c = center(agora);
        for (String side : List.of("south", "east", "west", "north")) {
            int[] d = dir(side);
            boolean alongX = d[1] != 0;
            int w = alongX ? ps.w() : ps.d();
            int dd = alongX ? ps.d() : ps.w();
            int x = d[0] < 0 ? agora.minX() - GAP - 1 - w / 2 : d[0] > 0 ? agora.maxX() + GAP + 1 + w / 2 : c.x();
            int z = d[1] < 0 ? agora.minZ() - GAP - 1 - dd / 2 : d[1] > 0 ? agora.maxZ() + GAP + 1 + dd / 2 : c.z();
            Box foot = new Box(x - w / 2, 0, z - dd / 2, x - w / 2 + w - 1, 0, z - dd / 2 + dd - 1);
            if (fits(foot, 4) && confirm(foot, 4)) {
                plot(id, ps.kind(), ps.stage(), foot, opposite(side));
                return true;
            }
        }
        return plotAround(id, ps, c, 16, 60, 5, false);
    }

    /** slope = true（剧场）：要坡地，高差 3–10。 */
    private boolean plotAround(String id, PlotSpec ps, Pos c, int rMin, int rMax, int maxRelief, boolean slope) {
        beginStep();
        double phase = rnd.nextDouble() * 360;
        for (int r = rMin; r <= rMax; r += 3) {
            for (double deg = phase; deg < phase + 360; deg += 15) {
                int x = c.x() + (int) Math.round(r * Math.cos(Math.toRadians(deg)));
                int z = c.z() + (int) Math.round(r * Math.sin(Math.toRadians(deg)));
                String facing = toward(x, z, c.x(), c.z());
                boolean rotated = facing.equals("east") || facing.equals("west");
                int w = rotated ? ps.d() : ps.w();
                int d = rotated ? ps.w() : ps.d();
                Box foot = new Box(x - w / 2, 0, z - d / 2, x - w / 2 + w - 1, 0, z - d / 2 + d - 1);
                if (slope) {
                    HeightGrid.Footprint f = grid.footprint(foot);
                    if (f.relief() < 3 || !fits(foot, 10)) {
                        continue;
                    }
                } else if (!fits(foot, maxRelief) || !confirm(foot, maxRelief)) {
                    continue;
                }
                plot(id, ps.kind(), ps.stage(), foot, facing);
                return true;
            }
        }
        return false;
    }
}
